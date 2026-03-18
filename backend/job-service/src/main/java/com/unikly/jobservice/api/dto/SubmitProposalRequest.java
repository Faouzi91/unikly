package com.unikly.jobservice.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubmitProposalRequest(
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal proposedBudget,
        @Size(max = 5000) String coverLetter
) {}
