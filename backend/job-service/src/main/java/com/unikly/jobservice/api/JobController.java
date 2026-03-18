package com.unikly.jobservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.api.dto.CreateJobRequest;
import com.unikly.jobservice.api.dto.JobResponse;
import com.unikly.jobservice.api.dto.StatusTransitionRequest;
import com.unikly.jobservice.api.dto.UpdateJobRequest;
import com.unikly.jobservice.application.JobService;
import com.unikly.jobservice.domain.JobStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        UUID clientId = UserContext.getUserId();
        UserContext.requireRole("ROLE_CLIENT");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(clientId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<JobResponse>> listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobService.listJobs(status, skill, minBudget, maxBudget, sort, direction, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateJobRequest request) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.updateJob(id, clientId, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobResponse> transitionStatus(@PathVariable UUID id,
                                                         @Valid @RequestBody StatusTransitionRequest request) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.transitionStatus(id, userId, request.status()));
    }
}
