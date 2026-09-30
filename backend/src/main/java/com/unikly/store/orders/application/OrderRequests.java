package com.unikly.store.orders.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.unikly.store.orders.domain.OrderFulfillmentStatus;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class OrderRequests {
    private OrderRequests() {}
    public record Create(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 6, max = 30) String phone,
            @NotBlank @Size(max = 160) String addressLine1,
            @Size(max = 160) String addressLine2,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String region,
            @NotBlank @Size(max = 24) String postalCode,
            @NotBlank @Size(max = 80) String country,
            @NotEmpty @Size(max = 50) List<@Valid Item> items) {}
    public record Item(@NotBlank @Size(max = 36) String productId, @NotNull @Min(1) @Max(1000) Integer quantity) {}
    public record UpdateFulfillmentStatus(@NotNull OrderFulfillmentStatus status,
                                         @Size(max = 80) String carrierName,
                                         @Size(max = 2048) String trackingUrl) {}
}
