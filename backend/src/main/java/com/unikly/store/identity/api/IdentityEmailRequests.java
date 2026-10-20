package com.unikly.store.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class IdentityEmailRequests {
    private IdentityEmailRequests() {
    }

    public record EmailAddress(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record Token(@NotBlank @Size(min = 40, max = 128) String token) {
    }

    public record PasswordReset(
            @NotBlank @Size(min = 40, max = 128) String token,
            @NotBlank @Size(min = 12, max = 72) String newPassword) {
    }
}
