package com.unikly.store.cart.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CartViews {
    private CartViews() {}

    public record CartView(
            String id,
            Long buyerId,
            List<CartItemView> items,
            BigDecimal subtotal,
            int itemCount,
            Instant updatedAt
    ) {}

    public record CartItemView(
            String productId,
            String productName,
            BigDecimal unitPrice,
            String image,
            int quantity,
            int stockQuantity,
            BigDecimal lineTotal
    ) {}
}
