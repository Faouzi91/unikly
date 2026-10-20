package com.unikly.store.identity.application;

import java.util.Optional;

public interface IdentityAccountService {
    AccountIdentity requireByEmail(String email);

    Optional<AccountIdentity> findByEmail(String email);

    void updateDisplayName(String email, String displayName);

    void changePassword(String email, String currentPassword, String newPassword);

    enum AccountRole {
        BUYER,
        SELLER,
        ADMIN
    }

    record AccountIdentity(Long id, String email, String displayName, AccountRole role) {}
}
