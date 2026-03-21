package com.unikly.userservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload used to build a social authorization URL")
public record SocialAuthorizationRequest(
        @Schema(description = "Identity provider alias in Keycloak", example = "google")
        @NotBlank
        @Pattern(regexp = "google|facebook|microsoft")
        String provider,

        @Schema(description = "Frontend callback URL", example = "http://localhost:4200/auth/login")
        @NotBlank
        @Size(max = 1024)
        String redirectUri,

        @Schema(description = "Opaque CSRF state from frontend")
        @NotBlank
        @Size(max = 255)
        String state,

        @Schema(description = "PKCE code challenge (S256)")
        @NotBlank
        @Size(max = 255)
        String codeChallenge
) {}
