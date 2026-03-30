package com.unikly.common.events;

import java.time.Instant;
import java.util.UUID;

public record FreelancerInvitedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        UUID jobId,
        UUID clientId,
        UUID freelancerId,
        String jobTitle,
        String message
) implements BaseEvent {

    public FreelancerInvitedEvent {
        if (eventType == null) eventType = "FreelancerInvited";
    }
}
