package com.unikly.jobservice.domain;

import java.util.Map;
import java.util.Set;

public final class JobStateMachine {

    public static final Map<JobStatus, Set<JobStatus>> TRANSITIONS = Map.of(
            JobStatus.DRAFT,       Set.of(JobStatus.OPEN),
            JobStatus.OPEN,        Set.of(JobStatus.IN_PROGRESS, JobStatus.CANCELLED),
            JobStatus.IN_PROGRESS, Set.of(JobStatus.COMPLETED, JobStatus.DISPUTED),
            JobStatus.COMPLETED,   Set.of(JobStatus.CLOSED),
            JobStatus.DISPUTED,    Set.of(JobStatus.COMPLETED, JobStatus.REFUNDED)
    );

    private JobStateMachine() {}

    public static void validateTransition(JobStatus current, JobStatus target) {
        Set<JobStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStateTransitionException(current, target);
        }
    }
}
