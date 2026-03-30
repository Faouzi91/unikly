package com.unikly.messagingservice.application.port.out;
import com.unikly.messagingservice.domain.model.OutboxEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import com.unikly.messagingservice.domain.model.*;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
