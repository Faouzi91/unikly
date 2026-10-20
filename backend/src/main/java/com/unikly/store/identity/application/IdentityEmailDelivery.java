package com.unikly.store.identity.application;

public interface IdentityEmailDelivery {
    void send(IdentityEmailMessage message);
}
