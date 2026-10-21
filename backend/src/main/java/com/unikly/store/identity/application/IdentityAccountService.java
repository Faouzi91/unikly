package com.unikly.store.identity.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IdentityAccountService {
    AccountIdentity requireByEmail(String email);

    AuthenticatedUserView requireAuthenticatedUser(String email);

    Optional<AccountIdentity> findByEmail(String email);

    Optional<PublicSellerIdentity> findSellerIdentityById(Long id);

    Map<Long, PublicSellerIdentity> findSellerIdentitiesByIds(Collection<Long> ids);

    void updateDisplayName(String email, String displayName);

    void changePassword(String email, String currentPassword, String newPassword);

    enum AccountRole {
        BUYER,
        SELLER,
        ADMIN
    }

    record AccountIdentity(Long id, String email, String displayName, AccountRole role) {}

    record PublicSellerIdentity(Long id, String displayName) {}

    record AuthenticatedUserView(
            Long id,
            String email,
            String displayName,
            String role,
            boolean emailVerified,
            List<String> permissions) {}
}
