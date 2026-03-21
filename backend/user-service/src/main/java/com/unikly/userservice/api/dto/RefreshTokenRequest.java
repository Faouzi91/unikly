package com.unikly.userservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to refresh an access token")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token from previous login response")
        @NotBlank @Size(max = 4000) String refreshToken
) {}
