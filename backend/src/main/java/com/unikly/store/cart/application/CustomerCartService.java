package com.unikly.store.cart.application;

import com.unikly.store.cart.application.CartViews.CartItemView;
import com.unikly.store.cart.application.CartViews.CartView;
import com.unikly.store.cart.domain.CustomerCart;
import com.unikly.store.cart.domain.CustomerCartItem;
import com.unikly.store.cart.persistence.CustomerCartRepository;
import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerCartService {

    private final CustomerCartRepository carts;
    private final CatalogProductRepository products;
    private final StoreUserRepository users;

    public CustomerCartService(
            CustomerCartRepository carts,
            CatalogProductRepository products,
            StoreUserRepository users) {
        this.carts = carts;
        this.products = products;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CartView getCart(String email) {
        StoreUser buyer = buyer(email);
        return carts.findByBuyerId(buyer.getId())
                .map(this::toView)
                .orElseGet(() -> emptyView(buyer.getId()));
    }

    public CartView addItem(String email, CartRequests.AddItem request) {
        StoreUser buyer = buyer(email);
        CatalogProduct product = products.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        CustomerCart cart = carts.findByBuyerId(buyer.getId())
                .orElseGet(() -> new CustomerCart(UUID.randomUUID().toString(), buyer.getId()));

        int currentQty = cart.findItem(request.productId())
                .map(CustomerCartItem::getQuantity)
                .orElse(0);

        int newQty = currentQty + request.quantity();
        if (newQty > product.getStockQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add " + request.quantity() + " items. Available stock: "
                            + product.getStockQuantity() + " (current in cart: " + currentQty + ")");
        }

        cart.addItem(request.productId(), request.quantity());
        return toView(carts.save(cart));
    }

    public CartView updateItem(String email, String productId, CartRequests.UpdateItem request) {
        StoreUser buyer = buyer(email);
        CatalogProduct product = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        CustomerCart cart = carts.findByBuyerId(buyer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        if (cart.findItem(productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in cart");
        }

        if (request.quantity() > product.getStockQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Requested quantity exceeds available stock (" + product.getStockQuantity() + ")");
        }

        cart.updateItem(productId, request.quantity());
        return toView(carts.save(cart));
    }

    public CartView removeItem(String email, String productId) {
        StoreUser buyer = buyer(email);
        CustomerCart cart = carts.findByBuyerId(buyer.getId())
                .orElseGet(() -> new CustomerCart(UUID.randomUUID().toString(), buyer.getId()));

        cart.removeItem(productId);
        return toView(carts.save(cart));
    }

    public void clearCart(String email) {
        StoreUser buyer = buyer(email);
        carts.findByBuyerId(buyer.getId()).ifPresent(cart -> {
            cart.clear();
            carts.save(cart);
        });
    }

    public void clearCartByBuyerId(Long buyerId) {
        carts.findByBuyerId(buyerId).ifPresent(cart -> {
            cart.clear();
            carts.save(cart);
        });
    }

    public CartView mergeCart(String email, CartRequests.MergeRequest request) {
        StoreUser buyer = buyer(email);
        CustomerCart cart = carts.findByBuyerId(buyer.getId())
                .orElseGet(() -> new CustomerCart(UUID.randomUUID().toString(), buyer.getId()));

        for (CartRequests.MergeItem item : request.items()) {
            Optional<CatalogProduct> productOpt = products.findById(item.productId());
            if (productOpt.isEmpty()) {
                continue;
            }
            CatalogProduct product = productOpt.get();
            int stock = product.getStockQuantity();
            if (stock <= 0) {
                continue;
            }

            int existingQty = cart.findItem(item.productId())
                    .map(CustomerCartItem::getQuantity)
                    .orElse(0);

            int targetQty = Math.min(existingQty + item.quantity(), stock);
            if (targetQty > 0) {
                if (existingQty > 0) {
                    cart.updateItem(item.productId(), targetQty);
                } else {
                    cart.addItem(item.productId(), targetQty);
                }
            }
        }

        return toView(carts.save(cart));
    }

    private StoreUser buyer(String email) {
        StoreUser user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != StoreRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private CartView toView(CustomerCart cart) {
        if (cart == null || cart.getItems().isEmpty()) {
            Long buyerId = cart != null ? cart.getBuyerId() : null;
            String id = cart != null ? cart.getId() : null;
            Instant updatedAt = cart != null ? cart.getUpdatedAt() : Instant.now();
            return new CartView(id, buyerId, List.of(), BigDecimal.ZERO, 0, updatedAt);
        }

        List<String> productIds = cart.getItems().stream()
                .map(CustomerCartItem::getProductId)
                .toList();

        Map<String, CatalogProduct> productMap = products.findAllById(productIds).stream()
                .collect(Collectors.toMap(CatalogProduct::getId, Function.identity()));

        List<CartItemView> itemViews = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int itemCount = 0;

        for (CustomerCartItem item : cart.getItems()) {
            CatalogProduct product = productMap.get(item.getProductId());
            if (product != null) {
                BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemViews.add(new CartItemView(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getImage(),
                        item.getQuantity(),
                        product.getStockQuantity(),
                        lineTotal
                ));
                subtotal = subtotal.add(lineTotal);
                itemCount += item.getQuantity();
            } else {
                itemViews.add(new CartItemView(
                        item.getProductId(),
                        "Unavailable Product",
                        BigDecimal.ZERO,
                        "",
                        item.getQuantity(),
                        0,
                        BigDecimal.ZERO
                ));
                itemCount += item.getQuantity();
            }
        }

        return new CartView(cart.getId(), cart.getBuyerId(), itemViews, subtotal, itemCount, cart.getUpdatedAt());
    }

    private CartView emptyView(Long buyerId) {
        return new CartView(null, buyerId, List.of(), BigDecimal.ZERO, 0, Instant.now());
    }
}
