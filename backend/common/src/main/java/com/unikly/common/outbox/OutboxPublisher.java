package com.unikly.common.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class OutboxPublisher {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicLong pendingEventsCount;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.pendingEventsCount = meterRegistry.gauge("unikly_outbox_pending_events", new AtomicLong(0));
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        // Update the gauge periodically
        long totalPending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        pendingEventsCount.set(totalPending);

        List<OutboxEvent> pendingEvents =
                outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = resolveTopicFromEventType(event.getEventType());
                kafkaTemplate.send(topic, event.getId().toString(), event.getPayload()).get();

                event.setStatus(OutboxStatus.SENT);
                event.setSentAt(Instant.now());
                outboxRepository.save(event);

                log.info("Published outbox event: id={}, type={}, topic={}",
                        event.getId(), event.getEventType(), topic);
            } catch (Exception e) {
                event.incrementRetryCount();
                if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event permanently failed after {} retries: id={}, type={}",
                            MAX_RETRY_COUNT, event.getId(), event.getEventType(), e);
                } else {
                    log.warn("Outbox event publish failed, will retry: id={}, type={}, attempt={}",
                            event.getId(), event.getEventType(), event.getRetryCount(), e);
                }
                outboxRepository.save(event);
            }
        }
    }

    private String resolveTopicFromEventType(String eventType) {
        if (eventType.startsWith("Job") || eventType.startsWith("Proposal")) {
            return "job.events";
        } else if (eventType.startsWith("Payment") || eventType.startsWith("Escrow")) {
            return "payment.events";
        } else if (eventType.startsWith("JobMatched")) {
            return "matching.events";
        } else if (eventType.startsWith("Message")) {
            return "messaging.events";
        } else if (eventType.startsWith("UserProfile")) {
            return "user.events";
        }
        return "unknown.events";
    }
}
