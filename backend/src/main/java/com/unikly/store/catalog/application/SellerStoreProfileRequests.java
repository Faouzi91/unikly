package com.unikly.store.catalog.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SellerStoreProfileRequests {
    private SellerStoreProfileRequests() {}

    public record Upsert(
            @NotBlank @Size(min = 2, max = 80) String storeName,
            @Size(max = 140) String headline,
            @Size(max = 600) String description,
            @Size(max = 80) String location,
            @Email @Size(max = 160) String supportEmail) {}
}
