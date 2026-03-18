package com.unikly.jobservice.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateJobRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal budget,
        @NotBlank @Size(max = 3) String currency,
        @NotEmpty List<String> skills
) {}
