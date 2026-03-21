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
            outboxRepository.save(new OutboxEvent(event.eventType(), payload));
        } catch (Exception e) {
            log.error("Failed to serialize UserProfileUpdatedEvent", e);
        }
    }
}
