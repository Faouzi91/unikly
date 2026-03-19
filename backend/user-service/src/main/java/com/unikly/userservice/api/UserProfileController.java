package com.unikly.userservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.userservice.api.dto.UserProfileRequest;
import com.unikly.userservice.api.dto.UserProfileResponse;
import com.unikly.userservice.application.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Create or update current user's profile")
    @ApiResponse(responseCode = "200", description = "Profile saved")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(@Valid @RequestBody UserProfileRequest request) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(profileService.createOrUpdateProfile(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user's profile by ID")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    public ResponseEntity<UserProfileResponse> getProfile(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getProfile(id));
    }

    @GetMapping
    @Operation(summary = "Search freelancers", description = "Returns a paginated list of freelancer profiles, optionally filtered by skill")
    @ApiResponse(responseCode = "200", description = "Freelancers retrieved")
    public ResponseEntity<PageResponse<UserProfileResponse>> searchFreelancers(
            @Parameter(description = "Filter by skill") @RequestParam(required = false) String skill,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.searchFreelancers(skill, page, size));
    }
}
