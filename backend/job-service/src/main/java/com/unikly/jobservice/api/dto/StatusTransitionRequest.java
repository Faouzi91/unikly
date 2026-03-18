package com.unikly.jobservice.api.dto;

import com.unikly.jobservice.domain.JobStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(
        @NotNull JobStatus status
) {}
