package com.unikly.matchingservice.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.UserProfileUpdatedEvent;
import com.unikly.matchingservice.domain.FreelancerSkillCache;
import com.unikly.matchingservice.domain.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final FreelancerSkillCacheRepository freelancerSkillCacheRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.events", groupId = "matching-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);

            switch (event) {
                case UserProfileUpdatedEvent e -> handleUserProfileUpdated(e);
                default -> log.debug("Ignoring unhandled event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
            throw new RuntimeException("Failed to process user event", e);
        }
    }

    @Transactional
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate UserProfileUpdatedEvent, skipping: eventId={}", event.eventId());
            return;
        }

        var cache = freelancerSkillCacheRepository.findById(event.userId())
                .orElse(FreelancerSkillCache.builder()
                        .userId(event.userId())
                        .skills(Collections.emptyList())
                        .updatedAt(Instant.now())
                        .build());

        if (event.skills() != null) {
            cache.setSkills(event.skills());
        }
        cache.setUpdatedAt(Instant.now());

        freelancerSkillCacheRepository.save(cache);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));

        log.info("Updated FreelancerSkillCache for userId={}, skills={}", event.userId(),
                event.skills() != null ? event.skills().size() : 0);
    }
}
