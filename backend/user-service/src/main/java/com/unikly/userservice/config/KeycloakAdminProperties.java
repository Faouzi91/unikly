package com.unikly.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String url,
        String realm,
        String adminRealm,
        String clientId,
        String username,
        String password
) {}
