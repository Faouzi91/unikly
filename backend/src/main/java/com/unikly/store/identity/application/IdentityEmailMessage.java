package com.unikly.store.identity.application;

public record IdentityEmailMessage(String recipient, String subject, String text) {
}
