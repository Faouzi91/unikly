package com.unikly.userservice.adapter.in.web;

import com.unikly.userservice.adapter.in.web.dto.RegistrationRequest;
import com.unikly.userservice.application.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Public registration endpoint")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    @SecurityRequirements  // marks as public in OpenAPI (no auth required)
    @Operation(summary = "Register a new user account")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<Void> register(@Valid @RequestBody RegistrationRequest request) {
        registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
