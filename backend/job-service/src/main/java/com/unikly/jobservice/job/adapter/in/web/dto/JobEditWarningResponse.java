package com.unikly.jobservice.job.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Warning returned when editing a job that has active proposals")
public record JobEditWarningResponse(
        @Schema(description = "Sensitive fields that were changed", example = "[\"budget\", \"skills\"]")
        List<String> sensitiveFieldsChanged,

        @Schema(description = "Number of proposals currently active on this job")
        int activeProposalCount,

        @Schema(description = "Human-readable warning message",
                example = "This job has 5 active proposals. Changes may invalidate them.")
        String message
) {}
