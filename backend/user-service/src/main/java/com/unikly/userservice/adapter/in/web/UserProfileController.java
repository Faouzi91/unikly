package com.unikly.userservice.adapter.in.web;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.userservice.adapter.in.web.dto.UserProfileRequest;
import com.unikly.userservice.adapter.in.web.dto.UserProfileResponse;
import com.unikly.userservice.application.service.StorageService;
import com.unikly.userservice.application.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserProfileController {

    private final UserProfileService profileService;
    private final StorageService storageService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved or auto-created")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(Authentication authentication) {
        UUID userId = UserContext.getUserId();
        Jwt jwt = authentication != null && authentication.getPrincipal() instanceof Jwt j ? j : null;
        return ResponseEntity.ok(profileService.getOrCreateProfile(userId, jwt));
    }

    @PutMapping("/me")
    @Operation(summary = "Create or update current user's profile")
    @ApiResponse(responseCode = "200", description = "Profile saved")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(@Valid @RequestBody UserProfileRequest request) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(profileService.createOrUpdateProfile(userId, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile avatar photo")
    @ApiResponse(responseCode = "200", description = "Avatar uploaded, returns avatarUrl")
    @ApiResponse(responseCode = "400", description = "Invalid file type or size")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        UUID userId = UserContext.getUserId();
        String avatarUrl = storageService.uploadAvatar(userId, file);
        profileService.updateAvatarUrl(userId, avatarUrl);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user's profile by ID")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "404", description = "Profile not found")
    public ResponseEntity<UserProfileResponse> getProfile(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getProfile(id));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get total registered users for admin dashboard")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        return ResponseEntity.ok(Map.of("totalUsers", profileService.getTotalUsers()));
    }

    @GetMapping("/admin/directory")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get paginated user directory for admin dashboard")
    public ResponseEntity<PageResponse<UserProfileResponse>> getAdminDirectory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.getAllUsers(page, size));
    }

    @GetMapping
    @Operation(summary = "Search freelancers")
    @ApiResponse(responseCode = "200", description = "Freelancers retrieved")
    public ResponseEntity<PageResponse<UserProfileResponse>> searchFreelancers(
            @Parameter(description = "Filter by skill") @RequestParam(required = false) String skill,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.searchFreelancers(skill, page, size));
    }

    @PostMapping("/admin/reindex-all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Re-index all freelancer profiles into Elasticsearch",
               description = "Publishes UserProfileUpdatedEvent for every freelancer, causing the search-service to re-index them. Admin only.")
    @ApiResponse(responseCode = "200", description = "Reindex queued")
    public ResponseEntity<Map<String, Integer>> reindexAllFreelancers() {
        int count = profileService.reindexAllFreelancers();
        return ResponseEntity.ok(Map.of("queued", count));
    }
}
