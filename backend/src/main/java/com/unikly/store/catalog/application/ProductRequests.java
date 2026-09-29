package com.unikly.store.catalog.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ProductRequests {
    private ProductRequests() {}

    public record Upsert(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Size(min = 10, max = 1000) String description,
            @NotBlank @Size(max = 40) String category,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @Size(max = 2000) String image,
            @Min(0) Integer stockQuantity) {}
}
