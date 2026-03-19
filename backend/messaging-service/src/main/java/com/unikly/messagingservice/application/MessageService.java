package com.unikly.messagingservice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.messagingservice.domain.*;
import com.unikly.messagingservice.infrastructure.MessagingPresenceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MessagingPresenceManager presenceManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public MessageDto sendMessage(UUID conversationId, UUID senderId,
                                  String content, MessageContentType contentType) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.hasParticipant(senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender is not a participant");
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setContentType(contentType);
        final Message savedMessage = messageRepository.save(message);

        // Update conversation's lastMessageAt
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        // Push to all other participants via WebSocket if online
        MessageDto dto = toDto(savedMessage);
        conversation.getParticipantIds().stream()
                .filter(pid -> !pid.equals(senderId))
                .forEach(recipientId -> {
                    if (presenceManager.isOnline(recipientId.toString())) {
                        messagingTemplate.convertAndSendToUser(
                                recipientId.toString(),
                                "/queue/messages",
                                dto
                        );
                        log.debug("Pushed message {} to online user {}", savedMessage.getId(), recipientId);
                    }
                });

        // Publish MessageSentEvent via outbox
        publishOutboxEvent(savedMessage, content, conversationId, senderId);

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<MessageDto> getMessages(UUID conversationId, UUID userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(page, size))
                .map(this::toDto);
    }

    @Transactional
    public void markAsRead(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        // Only the recipient (not the sender) can mark as read
        if (message.getSenderId().equals(userId)) {
            return;
        }

        Conversation conversation = conversationRepository.findById(message.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (message.getReadAt() == null) {
            message.setReadAt(Instant.now());
            messageRepository.save(message);
        }
    }

    private void publishOutboxEvent(Message message, String content,
                                     UUID conversationId, UUID senderId) {
        String preview = content.length() > 100 ? content.substring(0, 100) : content;
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "messageId", message.getId().toString(),
                    "conversationId", conversationId.toString(),
                    "senderId", senderId.toString(),
                    "preview", preview,
                    "createdAt", message.getCreatedAt().toString()
            ));

            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("Message");
            outbox.setAggregateId(message.getId());
            outbox.setEventType("MessageSentEvent");
            outbox.setPayload(payload);
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event for message {}", message.getId(), e);
        }
    }

    private MessageDto toDto(Message m) {
        return new MessageDto(m.getId(), m.getConversationId(), m.getSenderId(),
                m.getContent(), m.getContentType(), m.getReadAt(), m.getCreatedAt());
    }
}
