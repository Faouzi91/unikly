package com.unikly.common.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = JobCreatedEvent.class, name = "JobCreated"),
        @JsonSubTypes.Type(value = JobStatusChangedEvent.class, name = "JobStatusChanged"),
        @JsonSubTypes.Type(value = ProposalSubmittedEvent.class, name = "ProposalSubmitted"),
        @JsonSubTypes.Type(value = ProposalAcceptedEvent.class, name = "ProposalAccepted"),
        @JsonSubTypes.Type(value = PaymentInitiatedEvent.class, name = "PaymentInitiated"),
        @JsonSubTypes.Type(value = PaymentCompletedEvent.class, name = "PaymentCompleted"),
        @JsonSubTypes.Type(value = EscrowReleasedEvent.class, name = "EscrowReleased"),
        @JsonSubTypes.Type(value = JobMatchedEvent.class, name = "JobMatched"),
        @JsonSubTypes.Type(value = MessageSentEvent.class, name = "MessageSent"),
        @JsonSubTypes.Type(value = UserProfileUpdatedEvent.class, name = "UserProfileUpdated"),
        @JsonSubTypes.Type(value = JobPublishedEvent.class, name = "JobPublished"),
        @JsonSubTypes.Type(value = JobUpdatedEvent.class, name = "JobUpdated"),
        @JsonSubTypes.Type(value = JobCancelledEvent.class, name = "JobCancelled"),
        @JsonSubTypes.Type(value = FreelancerInvitedEvent.class, name = "FreelancerInvited")
})
public sealed interface BaseEvent permits
        JobCreatedEvent,
        JobUpdatedEvent,
        JobPublishedEvent,
        JobCancelledEvent,
        JobStatusChangedEvent,
        ProposalSubmittedEvent,
        ProposalAcceptedEvent,
        JobMatchedEvent,
        MessageSentEvent,
        PaymentInitiatedEvent,
        PaymentCompletedEvent,
        EscrowReleasedEvent,
        UserProfileUpdatedEvent,
        FreelancerInvitedEvent {

    UUID eventId();

    String eventType();

    Instant timestamp();
}
