package com.unikly.jobservice.infrastructure;

import com.unikly.jobservice.domain.Job;
import com.unikly.jobservice.domain.JobStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> hasSkill(String skill) {
        return (root, query, cb) -> skill == null || skill.isBlank()
                ? null
                : cb.isMember(skill, root.get("skills"));
    }

    public static Specification<Job> minBudget(BigDecimal min) {
        return (root, query, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("budget"), min);
    }

    public static Specification<Job> maxBudget(BigDecimal max) {
        return (root, query, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("budget"), max);
    }
}
