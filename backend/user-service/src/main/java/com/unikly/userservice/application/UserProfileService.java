package com.unikly.userservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.dto.PageResponse;
import com.unikly.common.events.UserProfileUpdatedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.userservice.api.dto.UserProfileRequest;
import com.unikly.userservice.api.dto.UserProfileResponse;
import com.unikly.userservice.api.mapper.UserProfileMapper;
import com.unikly.userservice.domain.UserProfile;
import com.unikly.userservice.domain.UserRole;
import com.unikly.userservice.infrastructure.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final OutboxRepository outboxRepository;
    private final UserProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserProfileResponse createOrUpdateProfile(UUID userId, UserProfileRequest request) {
        var profile = profileRepository.findById(userId).orElse(null);

        if (profile == null) {
            profile = UserProfile.builder()
                    .id(userId)
                    .displayName(request.displayName())
                    .bio(request.bio())
                    .avatarUrl(request.avatarUrl())
                    .role(request.role())
                    .skills(request.skills())
                    .hourlyRate(request.hourlyRate())
                    .currency(request.currency())
                    .location(request.location())
                    .portfolioLinks(request.portfolioLinks())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        } else {
            profileMapper.updateEntity(request, profile);
            profile.setUpdatedAt(Instant.now());
        }

        profile = profileRepository.save(profile);
        publishProfileUpdatedEvent(profile);

        log.info("Profile created/updated for userId={}", userId);
        return profileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        var profile = profileRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found: " + userId));
        return profileMapper.toResponse(profile);
    }

    /**
     * Returns the profile for the given user, auto-creating a minimal one from JWT claims
     * if no profile exists yet (e.g. test users seeded directly in Keycloak).
     */
    @Transactional
    public UserProfileResponse getOrCreateProfile(UUID userId, Jwt jwt) {
        return profileRepository.findById(userId)
                .map(profileMapper::toResponse)
                .orElseGet(() -> {
                    String displayName = jwt != null ? buildDisplayName(jwt) : "User";
                    UserRole role = jwt != null ? resolveRole(jwt) : UserRole.CLIENT;

                    var profile = UserProfile.builder()
                            .id(userId)
                            .displayName(displayName)
                            .role(role)
                            .skills(List.of())
                            .portfolioLinks(List.of())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    profile = profileRepository.save(profile);
                    publishProfileUpdatedEvent(profile);
                    log.info("Auto-created profile for userId={}", userId);
                    return profileMapper.toResponse(profile);
                });
    }

    private String buildDisplayName(Jwt jwt) {
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        if (given != null && family != null) return given + " " + family;
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) return name;
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : "User";
    }

    private UserRole resolveRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            var roles = (java.util.List<String>) realmAccess.get("roles");
            if (roles.contains("ROLE_ADMIN")) return UserRole.ADMIN;
            if (roles.contains("ROLE_FREELANCER")) return UserRole.FREELANCER;
        }
        return UserRole.CLIENT;
    }

    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return profileRepository.count();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> getAllUsers(int page, int size) {
        var result = profileRepository.findAll(PageRequest.of(page, size));
        var content = result.getContent().stream()
                .map(profileMapper::toResponse)
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void updateAvatarUrl(UUID userId, String avatarUrl) {
        var profile = profileRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found: " + userId));
        profile.setAvatarUrl(avatarUrl);
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);
        log.info("Avatar URL updated for userId={}", userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> searchFreelancers(String skill, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result = (skill != null && !skill.isBlank())
                ? profileRepository.findByRoleAndSkill(UserRole.FREELANCER.name(), skill, pageable)
                : profileRepository.findByRole(UserRole.FREELANCER, pageable);

        var content = result.getContent().stream()
                .map(profileMapper::toResponse)
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private void publishProfileUpdatedEvent(UserProfile profile) {
        try {
            var event = new UserProfileUpdatedEvent(
                    UUID.randomUUID(),
                    null,
                    Instant.now(),
                    profile.getId(),
                    List.of("displayName", "skills", "bio"),
                    profile.getSkills() != null ? profile.getSkills() : List.of()
            );
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(event.eventType(), profile.getId(), "UserProfile", payload));
        } catch (Exception e) {
            log.error("Failed to serialize UserProfileUpdatedEvent", e);
        }
    }
}
