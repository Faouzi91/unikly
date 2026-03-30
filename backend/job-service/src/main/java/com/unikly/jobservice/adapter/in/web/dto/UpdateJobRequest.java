package com.unikly.jobservice.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request to partially update a job posting — only non-null fields are applied")
public record UpdateJobRequest(

        @Schema(description = "Updated job title", example = "Build Angular Dashboard v2", maxLength = 200)
        @Size(max = 200)
        String title,

        @Schema(description = "Updated job description")
        String description,

        @Schema(description = "Updated fixed budget amount", example = "2000.00")
        @Positive
        BigDecimal budget,

        @Schema(description = "Updated required skills", example = "[\"Angular\", \"RxJS\"]")
        List<String> skills
) {}
