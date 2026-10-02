package com.unikly.store.orders.application;

import com.unikly.store.cart.application.CustomerCartService;
import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.orders.domain.CustomerOrder;
import com.unikly.store.orders.domain.CustomerOrderItem;
import com.unikly.store.orders.domain.DeliveryMethod;
import com.unikly.store.orders.domain.OrderFulfillmentStatus;
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
    private final CustomerCartService carts;

    public CustomerOrderService(
            CustomerOrderRepository orders,
            CatalogProductRepository products,
            StoreUserRepository users,
            CustomerCartService carts) {
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.carts = carts;
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
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> line : requested.entrySet()) {
            CatalogProduct product = products.findByIdForUpdate(line.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A product is no longer available"));
            if (line.getValue() > product.getStockQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        product.getName() + " has only " + product.getStockQuantity() + " left in stock");
            }
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(line.getValue())));
            lockedProducts.add(product);
        }

        DeliveryMethod method = parseDeliveryMethod(request.deliveryMethod());
        BigDecimal deliveryFee = method.calculateFee(subtotal);
        BigDecimal total = subtotal.add(deliveryFee);

        String id = UUID.randomUUID().toString();
        String reference = "UK-" + id.substring(0, 8).replace("-", "").toUpperCase();
        CustomerOrder order = new CustomerOrder(id, reference, buyer.getId(), clean(request.fullName()),
                clean(request.email()).toLowerCase(), clean(request.phone()), clean(request.addressLine1()),
                optional(request.addressLine2()), clean(request.city()), clean(request.region()),
                clean(request.postalCode()), clean(request.country()), method.name(), deliveryFee, total);
        for (int index = 0; index < lockedProducts.size(); index++) {
            CatalogProduct product = lockedProducts.get(index);
            int quantity = requested.get(product.getId());
            product.decreaseStock(quantity);
            order.addItem(new CustomerOrderItem(product.getId(), product.getName(), product.getSellerId(),
                    quantity, product.getPrice()));
        }
        CustomerOrder saved = orders.save(order);
        carts.clearCartByBuyerId(buyer.getId());
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderView> listMine(String email) {
        StoreUser buyer = buyer(email);
        return orders.findAllByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream().map(this::view).toList();
    }

    public OrderView confirmDelivery(String email, String reference) {
        StoreUser buyer = buyer(email);
        CustomerOrder order = orders.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!buyer.getId().equals(order.getBuyerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (order.getItems().isEmpty() || order.getItems().stream()
                .anyMatch(item -> item.getFulfillmentStatus().ordinal() < OrderFulfillmentStatus.SHIPPED.ordinal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Delivery can be confirmed after every item in the order has shipped");
        }
        order.getItems().stream()
                .filter(item -> item.getFulfillmentStatus() == OrderFulfillmentStatus.SHIPPED)
                .forEach(item -> item.advanceFulfillmentStatus(OrderFulfillmentStatus.DELIVERED));
        order.updateStatus(OrderFulfillmentStatus.DELIVERED.name());
        return view(orders.save(order));
    }

    public OrderView cancel(String email, String reference) {
        StoreUser buyer = buyer(email);
        CustomerOrder order = orders.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!buyer.getId().equals(order.getBuyerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        if (order.getItems().isEmpty() || order.getItems().stream()
                .anyMatch(item -> item.getFulfillmentStatus() != OrderFulfillmentStatus.PLACED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only orders that have not entered processing can be canceled");
        }
        for (CustomerOrderItem item : order.getItems()) {
            products.findByIdForUpdate(item.getProductId()).ifPresent(product -> product.increaseStock(item.getQuantity()));
            item.cancel();
        }
        order.updateStatus(OrderFulfillmentStatus.CANCELED.name());
        return view(orders.save(order));
    }

    @Transactional(readOnly = true)
    public List<SellerOrderView> listForSeller(String email) {
        StoreUser seller = seller(email);
        return orders.findAllForSeller(seller.getId()).stream()
                .map(order -> sellerView(order, seller.getId()))
                .toList();
    }

    public SellerOrderView updateSellerFulfillment(
            String email, String reference, OrderRequests.UpdateFulfillmentStatus request) {
        OrderFulfillmentStatus targetStatus = request.status();
        StoreUser seller = seller(email);
        CustomerOrder order = orders.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        List<CustomerOrderItem> sellerItems = order.getItems().stream()
                .filter(item -> seller.getId().equals(item.getSellerId()))
                .toList();
        if (sellerItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        OrderFulfillmentStatus currentStatus = sellerItems.getFirst().getFulfillmentStatus();
        if (sellerItems.stream().anyMatch(item -> item.getFulfillmentStatus() != currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seller order items have different fulfillment statuses");
        }
        if (currentStatus.next() != targetStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fulfillment must advance one step at a time");
        }
        String carrier = optional(request.carrierName());
        String trackingUrl = optional(request.trackingUrl());
        if (targetStatus == OrderFulfillmentStatus.SHIPPED) {
            if (carrier == null || trackingUrl == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrier and tracking link are required when shipping");
            }
            try {
                java.net.URI uri = java.net.URI.create(trackingUrl);
                String scheme = uri.getScheme();
                if (!uri.isAbsolute() || uri.getHost() == null ||
                        !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking link must be a valid HTTP or HTTPS URL");
            }
            sellerItems.forEach(item -> item.setShippingDetails(carrier, trackingUrl));
        }
        sellerItems.forEach(item -> item.advanceFulfillmentStatus(targetStatus));
        OrderFulfillmentStatus overallStatus = order.getItems().stream()
                .map(CustomerOrderItem::getFulfillmentStatus)
                .min(java.util.Comparator.comparingInt(OrderFulfillmentStatus::ordinal))
                .orElse(OrderFulfillmentStatus.PLACED);
        order.updateStatus(overallStatus.name());
        orders.save(order);
        return sellerView(order, seller.getId());
    }

    private StoreUser seller(String email) {
        StoreUser user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != StoreRole.SELLER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    private StoreUser buyer(String email) {
        StoreUser user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != StoreRole.BUYER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    private SellerOrderView sellerView(CustomerOrder order, Long sellerId) {
        List<CustomerOrderItem> sellerItems = order.getItems().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .toList();
        List<SellerOrderItemView> items = sellerItems.stream()
                .map(item -> new SellerOrderItemView(item.getProductId(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice(), item.getFulfillmentStatus().name(), item.getCarrierName(), item.getTrackingUrl()))
                .toList();
        BigDecimal subtotal = sellerItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        OrderFulfillmentStatus status = sellerItems.stream()
                .map(CustomerOrderItem::getFulfillmentStatus)
                .min(java.util.Comparator.comparingInt(OrderFulfillmentStatus::ordinal))
                .orElse(OrderFulfillmentStatus.PLACED);
        return new SellerOrderView(order.getReference(), order.getCreatedAt(), status.name(), order.getFullName(),
                order.getEmail(), order.getPhone(), order.getAddressLine1(), order.getAddressLine2(),
                order.getCity(), order.getRegion(), order.getPostalCode(), order.getCountry(),
                order.getDeliveryMethod(), subtotal, items);
    }

    private OrderView view(CustomerOrder order) {
        List<OrderItemView> items = order.getItems().stream()
                .map(item -> new OrderItemView(item.getProductId(), item.getProductName(), item.getQuantity(),
                        item.getUnitPrice(), item.getSellerId(), item.getFulfillmentStatus().name(),
                        item.getCarrierName(), item.getTrackingUrl()))
                .toList();
        return new OrderView(order.getReference(), order.getStatus(), order.getCreatedAt(),
                order.getDeliveryMethod(), order.getDeliveryFee(), order.getTotal(), items);
    }

    private DeliveryMethod parseDeliveryMethod(String deliveryMethod) {
        if (deliveryMethod == null || deliveryMethod.isBlank()) {
            return DeliveryMethod.STANDARD;
        }
        try {
            return DeliveryMethod.valueOf(deliveryMethod.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported delivery method. Allowed methods: STANDARD, EXPRESS");
        }
    }

    private static String clean(String value) { return value.trim(); }
    private static String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record OrderView(String reference, String status, Instant createdAt, String deliveryMethod,
                            BigDecimal deliveryFee, BigDecimal total, List<OrderItemView> items) {}
    public record OrderItemView(String productId, String productName, int quantity, BigDecimal unitPrice,
                                Long sellerId, String status, String carrierName, String trackingUrl) {}
    public record SellerOrderView(String reference, Instant createdAt, String status, String fullName, String email,
                                  String phone, String addressLine1, String addressLine2, String city, String region,
                                  String postalCode, String country, String deliveryMethod, BigDecimal subtotal,
                                  List<SellerOrderItemView> items) {}
    public record SellerOrderItemView(String productId, String productName, int quantity, BigDecimal unitPrice,
                                      String status, String carrierName, String trackingUrl) {}
}
