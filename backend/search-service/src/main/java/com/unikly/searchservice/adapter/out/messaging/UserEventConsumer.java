package com.unikly.searchservice.adapter.out.messaging;
import com.unikly.searchservice.application.port.out.FreelancerDocumentRepository;
import com.unikly.searchservice.application.port.out.ProcessedEventDocumentRepository;
import com.unikly.searchservice.adapter.out.provider.UserProfileClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.UserProfileUpdatedEvent;
import com.unikly.searchservice.domain.model.FreelancerDocument;
import com.unikly.searchservice.domain.model.ProcessedEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final FreelancerDocumentRepository freelancerDocumentRepository;
    private final ProcessedEventDocumentRepository processedEventRepository;
    private final UserProfileClient userProfileClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.events", groupId = "search-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);

            if (event instanceof UserProfileUpdatedEvent e) {
                handleUserProfileUpdated(e);
            } else {
                log.debug("Ignoring unhandled event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
            throw new RuntimeException("Failed to process user event", e);
        }
    }

    private void handleUserProfileUpdated(UserProfileUpdatedEvent event) {
        if (isAlreadyProcessed(event.eventId().toString())) {
            log.info("Duplicate UserProfileUpdatedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        try {
            var profile = userProfileClient.fetchProfile(event.userId().toString());
            if (profile == null) {
                log.warn("Could not fetch profile for userId={}, using event data only", event.userId());
                updateFromEventData(event);
            } else {
                var doc = FreelancerDocument.builder()
                        .userId(event.userId().toString())
                        .displayName(profile.displayName())
                        .skills(profile.skills())
                        .hourlyRate(profile.hourlyRate() != null ? profile.hourlyRate().doubleValue() : 0)
                        .averageRating(profile.averageRating() != null ? profile.averageRating() : 0)
                        .location(profile.location())
                        .bio(profile.bio())
                        .build();
                freelancerDocumentRepository.save(doc);
                log.info("Re-indexed freelancer: userId={}", event.userId());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch profile for userId={}, using event data", event.userId(), e);
            updateFromEventData(event);
        }

        markProcessed(event.eventId().toString());
    }

    private void updateFromEventData(UserProfileUpdatedEvent event) {
        var existing = freelancerDocumentRepository.findById(event.userId().toString())
                .orElse(FreelancerDocument.builder()
                        .userId(event.userId().toString())
                        .build());

        if (event.skills() != null) {
            existing.setSkills(event.skills());
        }
        freelancerDocumentRepository.save(existing);
        log.info("Updated freelancer from event data: userId={}", event.userId());
    }

    private boolean isAlreadyProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void markProcessed(String eventId) {
        processedEventRepository.save(new ProcessedEventDocument(eventId, Instant.now()));
    }
}
