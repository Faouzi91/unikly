package com.unikly.jobservice.adapter.in.web.dto;

import com.unikly.jobservice.domain.model.JobStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(
        @NotNull JobStatus status
) {}
