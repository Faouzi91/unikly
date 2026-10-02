package com.unikly.store.cart.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class CartRequests {
    private CartRequests() {}

    public record AddItem(
            @NotBlank(message = "Product ID is required")
            String productId,

            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {}

    public record UpdateItem(
            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {}

    public record MergeItem(
            @NotBlank(message = "Product ID is required")
            String productId,

            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {}

    public record MergeRequest(
            @NotNull(message = "Items list cannot be null")
            List<@Valid MergeItem> items
    ) {}
}
