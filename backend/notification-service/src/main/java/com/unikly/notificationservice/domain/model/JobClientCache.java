package com.unikly.notificationservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Local read-model that maps jobId → clientId, title, and freelancerId.
 * Populated from JobCreatedEvent and ProposalAcceptedEvent.
 * Used by Kafka consumers that need to look up client/freelancer for a job.
 */
@Entity
@Table(name = "job_client_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobClientCache {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(nullable = false, length = 200)
    private String title;

    /** Populated once a proposal is accepted. */
    @Column(name = "freelancer_id")
    private UUID freelancerId;
}
