package com.unikly.notificationservice.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.events.MessageSentEvent;
import com.unikly.notificationservice.application.NotificationDeliveryService;
import com.unikly.notificationservice.domain.NotificationType;
import com.unikly.notificationservice.domain.ProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingEventConsumer {

    private final NotificationDeliveryService deliveryService;
    private final WebSocketPresenceManager presenceManager;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "messaging.events", groupId = "notification-service-group")
    public void consume(String message) {
        try {
            var event = objectMapper.readValue(message, BaseEvent.class);
            switch (event) {
                case MessageSentEvent e -> handleMessageSent(e);
                default -> log.debug("Ignoring event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process messaging event: {}", message, e);
            throw new RuntimeException("Failed to process messaging event", e);
        }
    }

    @Transactional
    public void handleMessageSent(MessageSentEvent event) {
        if (processedEventRepository.existsById(event.eventId())) return;

        // Only notify if recipient is NOT currently active in this conversation.
        // The messaging service will set "user:{userId}:active-conversation" when a user
        // opens a conversation. Until then, we check general online presence.
        String activeConvKey = "user:" + event.recipientId() + ":active-conversation";
        boolean inConversation = Boolean.TRUE.equals(
                presenceManager.isKeyEqual(activeConvKey, event.conversationId().toString()));

        if (!inConversation) {
            String preview = event.preview() != null ? ": \"" + event.preview() + "\"" : "";
            deliveryService.createAndDeliver(
                    event.recipientId(),
                    NotificationType.MESSAGE_RECEIVED,
                    "New message",
                    "You have a new message" + preview,
                    "/messages/" + event.conversationId());
        } else {
            log.debug("Skipping notification — recipient is in conversation: userId={}, convId={}",
                    event.recipientId(), event.conversationId());
        }

        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
    }
}
