package com.unikly.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.auth")
public record KeycloakAuthProperties(
        String url,
        String realm,
        String clientId,
        String clientSecret
) {}
