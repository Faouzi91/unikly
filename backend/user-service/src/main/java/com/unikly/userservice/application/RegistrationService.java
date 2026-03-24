package com.unikly.userservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.UserProfileUpdatedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.userservice.api.dto.RegistrationRequest;
import com.unikly.userservice.config.KeycloakAdminProperties;
import com.unikly.userservice.domain.UserProfile;
import com.unikly.userservice.domain.UserRole;
import com.unikly.userservice.infrastructure.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RestClient keycloakRestClient;
    private final KeycloakAdminProperties keycloakProps;
    private final UserProfileRepository profileRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void register(RegistrationRequest request) {
        // 1. Obtain Keycloak admin access token
        String adminToken = fetchAdminToken();

        // 2. Create user in Keycloak — returns Location header with the new user ID
        String userId = createKeycloakUser(adminToken, request);

        // 3. Assign realm role in Keycloak so the JWT token carries the role
        assignRealmRole(adminToken, userId, "ROLE_" + request.role().name());

        // 4. Create initial profile in DB
        UserProfile profile = createUserProfile(UUID.fromString(userId), request);

        // 5. Publish outbox event so search-service indexes this profile
        publishProfileCreatedEvent(profile);

        log.info("Registered new user: email={}, role={}", request.email(), request.role());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String fetchAdminToken() {
        String tokenUrl = "%s/realms/%s/protocol/openid-connect/token"
                .formatted(keycloakProps.url(), keycloakProps.adminRealm());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProps.clientId());
        form.add("username", keycloakProps.username());
        form.add("password", keycloakProps.password());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = keycloakRestClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (body == null || !body.containsKey("access_token")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not obtain admin token");
        }
        return (String) body.get("access_token");
    }

    private String createKeycloakUser(String adminToken, RegistrationRequest request) {
        String usersUrl = "%s/admin/realms/%s/users".formatted(keycloakProps.url(), keycloakProps.realm());

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", request.password(),
                "temporary", false
        );

        Map<String, Object> user = Map.of(
                "username", request.email(),
                "email", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(credential)
        );

        try {
            var response = keycloakRestClient.post()
                    .uri(usersUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();

            // Keycloak returns 201 with Location: .../users/{uuid}
            var location = response.getHeaders().getLocation();
            if (location == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak returned no user ID");
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
            }
            log.error("Keycloak user creation failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Registration service unavailable");
        }
    }

    /**
     * Assigns a Keycloak realm role (e.g. "ROLE_CLIENT") to the newly created user.
     * Fetches the role representation first, then posts it to the role-mappings endpoint.
     */
    private void assignRealmRole(String adminToken, String userId, String roleName) {
        try {
            // 1. Fetch the role representation by name
            String roleUrl = "%s/admin/realms/%s/roles/%s"
                    .formatted(keycloakProps.url(), keycloakProps.realm(), roleName);

            @SuppressWarnings("unchecked")
            Map<String, Object> roleRep = keycloakRestClient.get()
                    .uri(roleUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);

            if (roleRep == null) {
                log.warn("Role '{}' not found in Keycloak — skipping role assignment", roleName);
                return;
            }

            // 2. Assign the role to the user
            String roleMappingsUrl = "%s/admin/realms/%s/users/%s/role-mappings/realm"
                    .formatted(keycloakProps.url(), keycloakProps.realm(), userId);

            keycloakRestClient.post()
                    .uri(roleMappingsUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRep))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Assigned role '{}' to Keycloak user {}", roleName, userId);
        } catch (Exception ex) {
            // Non-fatal: user is created, role assignment failure is logged
            log.error("Failed to assign role '{}' to Keycloak user {}: {}", roleName, userId, ex.getMessage());
        }
    }

    private UserProfile createUserProfile(UUID userId, RegistrationRequest request) {
        if (profileRepository.existsById(userId)) {
            return profileRepository.findById(userId).orElseThrow();
        }
        var profile = UserProfile.builder()
                .id(userId)
                .displayName(request.firstName() + " " + request.lastName())
                .role(request.role())
                .skills(List.of())
                .portfolioLinks(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return profileRepository.save(profile);
    }

    private void publishProfileCreatedEvent(UserProfile profile) {
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
            log.error("Failed to publish profile created event for userId={}", profile.getId(), e);
        }
    }
}
