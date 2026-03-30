package com.unikly.messagingservice.application.service;
import com.unikly.messagingservice.adapter.in.web.dto.GetOrCreateConversationRequest;
import com.unikly.messagingservice.adapter.in.web.dto.ConversationDto;

import com.unikly.messagingservice.domain.model.Conversation;
import com.unikly.messagingservice.application.port.out.ConversationRepository;
import com.unikly.messagingservice.application.port.out.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ConversationDto getOrCreateConversation(GetOrCreateConversationRequest request, UUID currentUserId) {
        UUID p1 = request.participantIds().get(0);
        UUID p2 = request.participantIds().get(1);

        Conversation conversation;
        if (request.jobId() != null) {
            conversation = conversationRepository
                    .findByJobAndParticipants(request.jobId(), p1, p2)
                    .orElseGet(() -> createConversation(request.participantIds(), request.jobId()));
        } else {
            conversation = conversationRepository
                    .findDirectConversation(p1, p2)
                    .orElseGet(() -> createConversation(request.participantIds(), null));
        }

        return toDto(conversation, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<ConversationDto> getUserConversations(UUID userId, int page) {
        return conversationRepository
                .findByParticipant(userId, PageRequest.of(page, 20))
                .map(c -> toDto(c, userId));
    }

    @Transactional(readOnly = true)
    public ConversationDto getById(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this conversation");
        }

        return toDto(conversation, userId);
    }

    @Transactional(readOnly = true)
    public long getTotalUnreadCount(UUID userId) {
        return messageRepository.findConversationIdsWithUnread(userId).size();
    }

    private Conversation createConversation(java.util.List<UUID> participantIds, UUID jobId) {
        Conversation c = new Conversation();
        c.setParticipantIds(participantIds);
        c.setJobId(jobId);
        Conversation saved = conversationRepository.save(c);
        log.info("Created conversation id={}, participants={}", saved.getId(), participantIds);
        return saved;
    }

    private ConversationDto toDto(Conversation c, UUID currentUserId) {
        long unread = messageRepository.countUnread(c.getId(), currentUserId);
        String preview = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(c.getId())
                .map(m -> m.getContent().length() > 80 ? m.getContent().substring(0, 80) + "…" : m.getContent())
                .orElse(null);
        return new ConversationDto(c.getId(), c.getJobId(), c.getParticipantIds(),
                c.getCreatedAt(), c.getLastMessageAt(), unread, preview);
    }
}
