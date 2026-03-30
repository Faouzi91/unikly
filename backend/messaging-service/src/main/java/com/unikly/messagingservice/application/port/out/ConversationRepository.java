package com.unikly.messagingservice.application.port.out;

import com.unikly.messagingservice.domain.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query(value = "SELECT * FROM conversations WHERE :userId = ANY(participant_ids) ORDER BY last_message_at DESC",
           countQuery = "SELECT count(*) FROM conversations WHERE :userId = ANY(participant_ids)",
           nativeQuery = true)
    Page<Conversation> findByParticipant(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = """
            SELECT * FROM conversations
            WHERE participant_ids @> ARRAY[:p1]::uuid[]
              AND participant_ids @> ARRAY[:p2]::uuid[]
              AND array_length(participant_ids, 1) = 2
            LIMIT 1
            """, nativeQuery = true)
    Optional<Conversation> findDirectConversation(@Param("p1") UUID p1, @Param("p2") UUID p2);

    @Query(value = """
            SELECT * FROM conversations
            WHERE job_id = :jobId
              AND participant_ids @> ARRAY[:p1]::uuid[]
              AND participant_ids @> ARRAY[:p2]::uuid[]
            LIMIT 1
            """, nativeQuery = true)
    Optional<Conversation> findByJobAndParticipants(@Param("jobId") UUID jobId,
                                                    @Param("p1") UUID p1,
                                                    @Param("p2") UUID p2);
}
