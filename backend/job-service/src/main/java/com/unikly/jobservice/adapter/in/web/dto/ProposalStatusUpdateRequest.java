package com.unikly.jobservice.adapter.in.web.dto;

import com.unikly.jobservice.domain.model.ProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(description = "Request to update the status of a proposal (client-side transitions only)")
public record ProposalStatusUpdateRequest(

        @Schema(description = "New status — must be one of: ACCEPTED, REJECTED", example = "ACCEPTED")
        @NotNull
        ProposalStatus newStatus
) {
    private static final Set<ProposalStatus> CLIENT_VALID_STATUSES = Set.of(
            ProposalStatus.ACCEPTED,
            ProposalStatus.REJECTED
    );

    @AssertTrue(message = "newStatus must be one of: ACCEPTED, REJECTED")
    public boolean isNewStatusClientValid() {
        return newStatus == null || CLIENT_VALID_STATUSES.contains(newStatus);
    }
}
