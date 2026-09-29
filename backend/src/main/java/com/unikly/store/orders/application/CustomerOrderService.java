package com.unikly.store.orders.application;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.orders.domain.CustomerOrder;
import com.unikly.store.orders.domain.CustomerOrderItem;
import com.unikly.store.orders.persistence.CustomerOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerOrderService {
    private final CustomerOrderRepository orders;
    private final CatalogProductRepository products;
    private final StoreUserRepository users;

    public CustomerOrderService(CustomerOrderRepository orders, CatalogProductRepository products, StoreUserRepository users) {
        this.orders = orders;
        this.products = products;
        this.users = users;
    }

    public OrderView create(String email, OrderRequests.Create request) {
        StoreUser buyer = buyer(email);
        Map<String, Integer> requested = new TreeMap<>();
        for (OrderRequests.Item item : request.items()) {
            if (requested.putIfAbsent(item.productId(), item.quantity()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each product may appear only once");
            }
        }

        List<CatalogProduct> lockedProducts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> line : requested.entrySet()) {
            CatalogProduct product = products.findByIdForUpdate(line.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A product is no longer available"));
            if (line.getValue() > product.getStockQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        product.getName() + " has only " + product.getStockQuantity() + " left in stock");
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.getValue())));
            lockedProducts.add(product);
        }

        String id = UUID.randomUUID().toString();
        String reference = "UK-" + id.substring(0, 8).replace("-", "").toUpperCase();
        CustomerOrder order = new CustomerOrder(id, reference, buyer.getId(), clean(request.fullName()),
                clean(request.email()).toLowerCase(), clean(request.phone()), clean(request.addressLine1()),
                optional(request.addressLine2()), clean(request.city()), clean(request.region()),
                clean(request.postalCode()), clean(request.country()), total);
        for (int index = 0; index < lockedProducts.size(); index++) {
            CatalogProduct product = lockedProducts.get(index);
            int quantity = requested.get(product.getId());
            product.decreaseStock(quantity);
            order.addItem(new CustomerOrderItem(product.getId(), product.getName(), product.getSellerId(),
                    quantity, product.getPrice()));
        }
        return view(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderView> listMine(String email) {
        StoreUser buyer = buyer(email);
        return orders.findAllByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream().map(this::view).toList();
    }

    private StoreUser buyer(String email) {
        StoreUser user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != StoreRole.BUYER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    private OrderView view(CustomerOrder order) {
        List<OrderItemView> items = order.getItems().stream()
                .map(item -> new OrderItemView(item.getProductId(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice(), item.getSellerId()))
                .toList();
        return new OrderView(order.getReference(), order.getStatus(), order.getCreatedAt(), order.getTotal(), items);
    }

    private static String clean(String value) { return value.trim(); }
    private static String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record OrderView(String reference, String status, Instant createdAt, BigDecimal total,
                            List<OrderItemView> items) {}
    public record OrderItemView(String productId, String productName, int quantity, BigDecimal unitPrice,
                                Long sellerId) {}
}
