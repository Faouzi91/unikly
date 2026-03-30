package com.unikly.userservice.adapter.in.web;

import com.unikly.userservice.adapter.in.web.dto.LoginRequest;
import com.unikly.userservice.adapter.in.web.dto.RefreshTokenRequest;
import com.unikly.userservice.adapter.in.web.dto.SocialAuthorizationRequest;
import com.unikly.userservice.adapter.in.web.dto.SocialCodeExchangeRequest;
import com.unikly.userservice.application.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Public authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Authenticate user with email/username and password")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token using refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid/expired")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request));
    }

    @PostMapping("/social/authorization-url")
    @SecurityRequirements
    @Operation(summary = "Build social login authorization URL")
    @ApiResponse(responseCode = "200", description = "Authorization URL generated")
    public ResponseEntity<Map<String, String>> socialAuthorizationUrl(
            @Valid @RequestBody SocialAuthorizationRequest request
    ) {
        return ResponseEntity.ok(Map.of("authorizationUrl", authenticationService.buildSocialAuthorizationUrl(request)));
    }

    @PostMapping("/social/exchange")
    @SecurityRequirements
    @Operation(summary = "Exchange social OAuth code for access and refresh tokens")
    @ApiResponse(responseCode = "200", description = "Authenticated with social provider")
    @ApiResponse(responseCode = "401", description = "Invalid or expired authorization code")
    public ResponseEntity<Map<String, Object>> socialExchange(@Valid @RequestBody SocialCodeExchangeRequest request) {
        return ResponseEntity.ok(authenticationService.exchangeSocialCode(request));
    }
}
