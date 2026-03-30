package com.unikly.jobservice.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request to submit a proposal for a job")
public record SubmitProposalRequest(

        @Schema(description = "Proposed budget for the job", example = "1200.00")
        @NotNull @Positive
        BigDecimal proposedBudget,

        @Schema(description = "Cover letter explaining qualifications and approach",
                example = "I have 5 years of Angular experience...", maxLength = 5000)
        @NotBlank @Size(max = 5000)
        String coverLetter
) {}
