package com.unikly.paymentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "webhook_events")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WebhookEvent {

    /** Stripe event ID — e.g. "evt_1234..." */
    @Id
    @Column(nullable = false, length = 255)
    private String id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    public static WebhookEvent of(String stripeEventId, String eventType) {
        WebhookEvent e = new WebhookEvent();
        e.id = stripeEventId;
        e.eventType = eventType;
        return e;
    }

    @PrePersist
    private void onCreate() {
        processedAt = Instant.now();
    }
}
