package com.unikly.jobservice.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateJobRequest(
        @Size(max = 200) String title,
        String description,
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal budget,
        @Size(max = 3) String currency,
        List<String> skills
) {}
