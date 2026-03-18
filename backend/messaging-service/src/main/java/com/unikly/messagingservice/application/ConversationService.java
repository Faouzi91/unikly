package com.unikly.messagingservice.application;

import com.unikly.messagingservice.domain.Conversation;
import com.unikly.messagingservice.domain.ConversationRepository;
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

    @Transactional
    public ConversationDto getOrCreateConversation(GetOrCreateConversationRequest request) {
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

        return toDto(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationDto> getUserConversations(UUID userId, int page) {
        return conversationRepository
                .findByParticipant(userId, PageRequest.of(page, 20))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ConversationDto getById(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this conversation");
        }

        return toDto(conversation);
    }

    private Conversation createConversation(java.util.List<UUID> participantIds, UUID jobId) {
        Conversation c = new Conversation();
        c.setParticipantIds(participantIds);
        c.setJobId(jobId);
        Conversation saved = conversationRepository.save(c);
        log.info("Created conversation id={}, participants={}", saved.getId(), participantIds);
        return saved;
    }

    private ConversationDto toDto(Conversation c) {
        return new ConversationDto(c.getId(), c.getJobId(), c.getParticipantIds(),
                c.getCreatedAt(), c.getLastMessageAt());
    }
}
