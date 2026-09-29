package com.unikly.store.identity.application;

import com.unikly.store.identity.domain.StoreUser;

public interface IdentityAccountService {
    StoreUser requireByEmail(String email);

    void updateDisplayName(String email, String displayName);

    void changePassword(String email, String currentPassword, String newPassword);
}