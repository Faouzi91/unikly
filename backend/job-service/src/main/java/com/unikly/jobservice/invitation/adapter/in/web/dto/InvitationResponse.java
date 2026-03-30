package com.unikly.jobservice.invitation.adapter.in.web.dto;

import com.unikly.jobservice.invitation.domain.model.Invitation;
import com.unikly.jobservice.invitation.domain.model.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID jobId,
        UUID clientId,
        UUID freelancerId,
        InvitationStatus status,
        String message,
        Instant createdAt
) {
    public static InvitationResponse from(Invitation inv) {
        return new InvitationResponse(
                inv.getId(), inv.getJobId(), inv.getClientId(),
                inv.getFreelancerId(), inv.getStatus(),
                inv.getMessage(), inv.getCreatedAt()
        );
    }
}
