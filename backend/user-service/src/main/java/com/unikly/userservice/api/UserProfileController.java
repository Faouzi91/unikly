package com.unikly.userservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.userservice.api.dto.UserProfileRequest;
import com.unikly.userservice.api.dto.UserProfileResponse;
import com.unikly.userservice.application.UserProfileService;
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
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(@Valid @RequestBody UserProfileRequest request) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(profileService.createOrUpdateProfile(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getProfile(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserProfileResponse>> searchFreelancers(
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(profileService.searchFreelancers(skill, page, size));
    }
}
