package com.unikly.userservice.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to authenticate with username/email and password")
public record LoginRequest(
        @Schema(description = "Email or username", example = "jane@example.com")
        @NotBlank @Size(max = 255) String username,

        @Schema(description = "Plain password", example = "Test1234!")
        @NotBlank @Size(min = 1, max = 200) String password
) {}
