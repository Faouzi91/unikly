package com.unikly.store.profile.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProfileRequests {
    private ProfileRequests() {
    }

    public record Update(
            @NotBlank @Size(min = 2, max = 100) String displayName,
            @Size(max = 32) @Pattern(regexp = "^$|^[+0-9() .-]{7,32}$") String phoneNumber,
            @Size(max = 120) String addressLine1,
            @Size(max = 120) String addressLine2,
            @Size(max = 80) String city,
            @Size(max = 80) String region,
            @Size(max = 24) String postalCode,
            @Pattern(regexp = "^$|^[A-Za-z]{2}$") String countryCode) {
    }

}