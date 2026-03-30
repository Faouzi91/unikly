package com.unikly.jobservice.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.events.BaseEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.jobservice.application.port.out.OutboxEventRepository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publish(String aggregateType, UUID aggregateId, BaseEvent event) {
        try {
            var outboxEvent = new OutboxEvent(
                    event.eventType(), 
                    aggregateId, 
                    aggregateType, 
                    objectMapper.writeValueAsString(event)
            );
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OutboxEvent: eventType={}, aggregateId={}", event.eventType(), aggregateId, e);
            throw new RuntimeException("Serialization error while publishing outbox event", e);
        }
    }
}
