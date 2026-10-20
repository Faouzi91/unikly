package com.unikly.store.cart.application;

import com.unikly.store.cart.application.CartViews.CartItemView;
import com.unikly.store.cart.application.CartViews.CartView;
import com.unikly.store.cart.domain.CustomerCart;
import com.unikly.store.cart.domain.CustomerCartItem;
import com.unikly.store.cart.persistence.CustomerCartRepository;
import com.unikly.store.catalog.application.CatalogProductService;
import com.unikly.store.catalog.application.CatalogProductService.ProductSnapshot;
import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.application.IdentityAccountService.AccountIdentity;
import com.unikly.store.identity.application.IdentityAccountService.AccountRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerCartService {

    private final CustomerCartRepository carts;
    private final CatalogProductService products;
    private final IdentityAccountService identity;

    public CustomerCartService(
            CustomerCartRepository carts,
            CatalogProductService products,
            IdentityAccountService identity) {
        this.carts = carts;
        this.products = products;
        this.identity = identity;
    }

    @Transactional(readOnly = true)
    public CartView getCart(String email) {
        AccountIdentity buyer = buyer(email);
        return carts.findByBuyerId(buyer.id())
                .map(this::toView)
                .orElseGet(() -> emptyView(buyer.id()));
    }

    public CartView addItem(String email, CartRequests.AddItem request) {
        AccountIdentity buyer = buyer(email);
        ProductSnapshot product = products.requireProduct(request.productId());

        CustomerCart cart = carts.findByBuyerId(buyer.id())
                .orElseGet(() -> new CustomerCart(UUID.randomUUID().toString(), buyer.id()));

        int currentQty = cart.findItem(request.productId())
                .map(CustomerCartItem::getQuantity)
                .orElse(0);

        int newQty = currentQty + request.quantity();
        if (newQty > product.stockQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot add " + request.quantity() + " items. Available stock: "
                            + product.stockQuantity() + " (current in cart: " + currentQty + ")");
        }

        cart.addItem(request.productId(), request.quantity());
        return toView(carts.save(cart));
    }

    public CartView updateItem(String email, String productId, CartRequests.UpdateItem request) {
        AccountIdentity buyer = buyer(email);
        ProductSnapshot product = products.requireProduct(productId);

        CustomerCart cart = carts.findByBuyerId(buyer.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        if (cart.findItem(productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found in cart");
        }

        if (request.quantity() > product.stockQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Requested quantity exceeds available stock (" + product.stockQuantity() + ")");
        }

        cart.updateItem(productId, request.quantity());
        return toView(carts.save(cart));
    }

    public CartView removeItem(String email, String productId) {
        AccountIdentity buyer = buyer(email);
        CustomerCart cart = carts.findByBuyerId(buyer.id())
                .orElseGet(() -> new CustomerCart(UUID.randomUUID().toString(), buyer.id()));

        cart.removeItem(productId);
        return toView(carts.save(cart));
    }

    public void clearCart(String email) {
        AccountIdentity buyer = buyer(email);
        carts.findByBuyerId(buyer.id()).ifPresent(cart -> {
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
        AccountIdentity buyer = buyer(email);
        CustomerCart cart = carts.findByBuyerId(buyer.id())
                .orElseGet(() -> new CustomerCart(UUID.randomUUID().toString(), buyer.id()));

        for (CartRequests.MergeItem item : request.items()) {
            Optional<ProductSnapshot> productOpt = products.findProduct(item.productId());
            if (productOpt.isEmpty()) {
                continue;
            }
            ProductSnapshot product = productOpt.get();
            int stock = product.stockQuantity();
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

    private AccountIdentity buyer(String email) {
        AccountIdentity user = identity.requireByEmail(email);
        if (user.role() != AccountRole.BUYER) {
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

        Map<String, ProductSnapshot> productMap = products.findProducts(productIds);

        List<CartItemView> itemViews = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int itemCount = 0;

        for (CustomerCartItem item : cart.getItems()) {
            ProductSnapshot product = productMap.get(item.getProductId());
            if (product != null) {
                BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemViews.add(new CartItemView(
                        product.id(),
                        product.name(),
                        product.price(),
                        product.image(),
                        item.getQuantity(),
                        product.stockQuantity(),
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
