package com.unikly.store.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.unikly.store.identity.domain.StoreRole;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record Register(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(min = 2, max = 100) String displayName,
            @NotNull RegistrationType accountType) {
    }

    public enum RegistrationType {
        BUYER,
        SELLER;

        public StoreRole role() {
            return switch (this) {
                case BUYER -> StoreRole.BUYER;
                case SELLER -> StoreRole.SELLER;
            };
        }
    }

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record ChangePassword(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 12, max = 72) String newPassword) {
    }
}