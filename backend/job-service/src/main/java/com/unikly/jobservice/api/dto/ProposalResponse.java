package com.unikly.jobservice.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unikly.jobservice.domain.ProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Proposal details")
public record ProposalResponse(
        @Schema(description = "Proposal unique identifier")
        UUID id,

        @Schema(description = "ID of the job this proposal was submitted for")
        UUID jobId,

        @Schema(description = "ID of the freelancer who submitted the proposal")
        UUID freelancerId,

        @Schema(description = "Proposed budget amount")
        BigDecimal proposedBudget,

        @Schema(description = "Cover letter")
        String coverLetter,

        @Schema(description = "Current proposal status")
        ProposalStatus status,

        @Schema(description = "Submission timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        Instant createdAt
) {}
