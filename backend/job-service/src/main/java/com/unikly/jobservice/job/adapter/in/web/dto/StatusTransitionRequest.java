package com.unikly.jobservice.job.adapter.in.web.dto;

import com.unikly.jobservice.job.domain.model.JobStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(
        @NotNull JobStatus status
) {}
