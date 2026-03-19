package com.unikly.jobservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request to submit a proposal for a job")
public record SubmitProposalRequest(
        @Schema(description = "Proposed budget for the job", example = "1200.00")
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal proposedBudget,

        @Schema(description = "Cover letter explaining qualifications and approach", example = "I have 5 years of Angular experience...", maxLength = 5000)
        @Size(max = 5000) String coverLetter
) {}
