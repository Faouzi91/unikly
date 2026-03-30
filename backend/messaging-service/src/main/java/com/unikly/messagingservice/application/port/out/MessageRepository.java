package com.unikly.messagingservice.application.port.out;

import com.unikly.messagingservice.domain.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.senderId <> :userId AND m.readAt IS NULL")
    long countUnread(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @Query(value = """
            SELECT DISTINCT m.conversation_id FROM messages m
            WHERE m.sender_id <> :userId
              AND m.read_at IS NULL
              AND m.conversation_id IN (SELECT c.id FROM conversations c WHERE :userId = ANY(c.participant_ids))
            """, nativeQuery = true)
    List<UUID> findConversationIdsWithUnread(@Param("userId") UUID userId);
}
