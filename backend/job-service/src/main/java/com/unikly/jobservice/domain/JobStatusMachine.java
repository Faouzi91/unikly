package com.unikly.jobservice.domain;

import java.util.Map;
import java.util.Set;

public final class JobStatusMachine {

    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS = Map.of(
            JobStatus.DRAFT, Set.of(JobStatus.OPEN),
            JobStatus.OPEN, Set.of(JobStatus.IN_PROGRESS, JobStatus.CANCELLED),
            JobStatus.IN_PROGRESS, Set.of(JobStatus.COMPLETED, JobStatus.DISPUTED),
            JobStatus.COMPLETED, Set.of(JobStatus.CLOSED),
            JobStatus.DISPUTED, Set.of(JobStatus.COMPLETED, JobStatus.REFUNDED)
    );

    private JobStatusMachine() {}

    public static void validateTransition(JobStatus current, JobStatus target) {
        Set<JobStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
    }
}
