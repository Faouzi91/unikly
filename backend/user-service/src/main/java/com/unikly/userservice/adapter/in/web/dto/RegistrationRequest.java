package com.unikly.userservice.adapter.in.web.dto;

import com.unikly.userservice.domain.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new user account")
public record RegistrationRequest(
        @Schema(description = "First name", example = "Jane")
        @NotBlank @Size(max = 50) String firstName,

        @Schema(description = "Last name", example = "Doe")
        @NotBlank @Size(max = 50) String lastName,

        @Schema(description = "Email address", example = "jane@example.com")
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(description = "Password (min 8 characters)")
        @NotBlank @Size(min = 8, max = 100) String password,

        @Schema(description = "Account role: CLIENT or FREELANCER")
        @NotNull UserRole role
) {}
