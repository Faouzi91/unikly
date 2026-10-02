package com.unikly.store.identity.domain;

public enum StorePermission {
    ACCOUNT_READ_SELF,
    ACCOUNT_UPDATE_SELF,
    CATALOG_READ,
    CART_MANAGE_SELF,
    ORDER_CREATE_SELF,
    ORDER_READ_SELF,
    ORDER_CANCEL_SELF,
    ORDER_FULFILL_OWN,
    PLATFORM_ADMIN,
    ACCOUNT_READ_ANY,
    ACCOUNT_SUSPEND,
    CATALOG_MANAGE,
    ORDER_MANAGE,
    ORDER_REFUND,
    REVIEW_CREATE_SELF,
    REVIEW_MODERATE,
    ROLE_ASSIGN,
    PLATFORM_REPORT_READ;

    public String authority() {
        return "PERM_" + name();
    }
}