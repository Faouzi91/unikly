package com.unikly.store.identity.domain;

import java.util.Set;

public enum StoreRole {
    BUYER(Set.of(
            StorePermission.ACCOUNT_READ_SELF,
            StorePermission.ACCOUNT_UPDATE_SELF,
            StorePermission.CATALOG_READ,
            StorePermission.CART_MANAGE_SELF,
            StorePermission.ORDER_CREATE_SELF,
            StorePermission.ORDER_READ_SELF,
            StorePermission.ORDER_CANCEL_SELF)),
    SELLER(Set.of(
            StorePermission.ACCOUNT_READ_SELF,
            StorePermission.ACCOUNT_UPDATE_SELF,
            StorePermission.CATALOG_READ,
            StorePermission.ORDER_FULFILL_OWN)),
    ADMIN(Set.of(
            StorePermission.ACCOUNT_READ_SELF,
            StorePermission.ACCOUNT_UPDATE_SELF,
            StorePermission.PLATFORM_ADMIN,
            StorePermission.ACCOUNT_READ_ANY,
            StorePermission.ACCOUNT_SUSPEND,
            StorePermission.CATALOG_MANAGE,
            StorePermission.ORDER_MANAGE,
            StorePermission.ORDER_REFUND,
            StorePermission.REVIEW_MODERATE,
            StorePermission.ROLE_ASSIGN,
            StorePermission.PLATFORM_REPORT_READ));

    private final Set<StorePermission> permissions;

    StoreRole(Set<StorePermission> permissions) {
        this.permissions = Set.copyOf(permissions);
    }

    public Set<StorePermission> permissions() {
        return permissions;
    }
}