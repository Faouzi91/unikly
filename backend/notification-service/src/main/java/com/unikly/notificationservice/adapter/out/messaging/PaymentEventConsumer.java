package com.unikly.notificationservice.adapter.out.messaging;
import com.unikly.notificationservice.application.port.out.JobClientCacheRepository;
import com.unikly.notificationservice.application.port.out.ProcessedEventRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.EscrowReleasedEvent;
import com.unikly.common.events.PaymentCompletedEvent;
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
public class PaymentEventConsumer {

    private final NotificationDeliveryService deliveryService;
    private final JobClientCacheRepository jobClientCacheRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.events", groupId = "notification-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);
            switch (event) {
                case PaymentCompletedEvent e -> handlePaymentCompleted(e);
                case EscrowReleasedEvent e   -> handleEscrowReleased(e);
                default -> log.debug("Ignoring event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", message, e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        // Look up freelancer from job cache (set when proposal was accepted)
        jobClientCacheRepository.findById(event.jobId()).ifPresentOrElse(
                job -> {
                    if (job.getFreelancerId() != null) {
                        deliveryService.createAndDeliver(
                                job.getFreelancerId(),
                                NotificationType.PAYMENT_FUNDED,
                                "Escrow funded",
                                "The client has funded escrow for your job. You can now start working!",
                                "/jobs/" + event.jobId());
                    }
                },
                () -> log.warn("JobClientCache miss for PaymentCompletedEvent: jobId={}", event.jobId()));

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }

    @Transactional
    public void handleEscrowReleased(EscrowReleasedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        String amountStr = event.amount() != null
                ? event.amount().amount() + " " + event.amount().currency()
                : "your payment";

        deliveryService.createAndDeliver(
                event.freelancerId(),
                NotificationType.ESCROW_RELEASED,
                "Payment released!",
                "Payment of " + amountStr + " has been released to your account.",
                "/jobs/" + event.jobId());

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
