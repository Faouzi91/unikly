package com.unikly.notificationservice.adapter.out.messaging;
import com.unikly.notificationservice.application.port.out.JobClientCacheRepository;
import com.unikly.notificationservice.application.port.out.ProcessedEventRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.JobMatchedEvent;
import com.unikly.notificationservice.application.service.NotificationDeliveryService;
import com.unikly.notificationservice.domain.model.NotificationType;
import com.unikly.notificationservice.domain.model.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEventConsumer {

    private final NotificationDeliveryService deliveryService;
    private final JobClientCacheRepository jobClientCacheRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "matching.events", groupId = "notification-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);
            switch (event) {
                case JobMatchedEvent e -> handleJobMatched(e);
                default -> log.debug("Ignoring event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process matching event: {}", message, e);
            throw new RuntimeException("Failed to process matching event", e);
        }
    }

    @Transactional
    public void handleJobMatched(JobMatchedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        int matchCount = event.matches() != null ? event.matches().size() : 0;

        jobClientCacheRepository.findById(event.jobId()).ifPresentOrElse(
                job -> deliveryService.createAndDeliver(
                        job.getClientId(),
                        NotificationType.JOB_MATCHED,
                        "Freelancers found for your job",
                        "We found " + matchCount + " matching freelancer" + (matchCount == 1 ? "" : "s") +
                                " for your job '" + job.getTitle() + "'.",
                        "/jobs/" + event.jobId()),
                () -> log.warn("JobClientCache miss for JobMatchedEvent: jobId={}", event.jobId()));

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
