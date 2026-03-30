package com.unikly.jobservice.job.adapter.in.web;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.job.adapter.in.web.dto.CreateJobRequest;
import com.unikly.jobservice.job.adapter.in.web.dto.JobResponse;
import com.unikly.jobservice.job.adapter.in.web.dto.StatusTransitionRequest;
import com.unikly.jobservice.job.adapter.in.web.dto.UpdateJobRequest;
import com.unikly.jobservice.job.application.service.JobService;
import com.unikly.jobservice.job.domain.model.EditDecision;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import org.springframework.data.domain.PageRequest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job lifecycle management")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a new job", description = "Creates a job posting. Requires CLIENT role.")
    @ApiResponse(responseCode = "201", description = "Job created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "403", description = "Forbidden — not a client")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        UUID clientId = UserContext.getUserId();
        UserContext.requireRole("ROLE_CLIENT");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(clientId, request));
    }

    @GetMapping
    @Operation(summary = "List my jobs", description = "Returns a paginated list of jobs owned by the current client")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved")
    public ResponseEntity<PageResponse<JobResponse>> listMyJobs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        UUID clientId = UserContext.getUserId();
        var result = jobService.getJobsByClient(clientId, PageRequest.of(page, size));
        return ResponseEntity.ok(new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job by ID")
    @ApiResponse(responseCode = "200", description = "Job found")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<JobResponse> getJob(
            @Parameter(description = "Job UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a job", description = "Partial update of job fields. Only the owning client may update.")
    @ApiResponse(responseCode = "200", description = "Job updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<JobResponse> updateJob(
            @Parameter(description = "Job UUID") @PathVariable UUID id,
            @Parameter(description = "Confirm sensitive changes") @RequestParam(defaultValue = "false") boolean confirmed,
            @Valid @RequestBody UpdateJobRequest request) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.updateJob(id, clientId, request, confirmed));
    }

    @PostMapping("/{id}/check-edit")
    @Operation(summary = "Check job edit eligibility",
               description = "Returns an EditDecision describing whether the edit is allowed and its impact on active proposals")
    @ApiResponse(responseCode = "200", description = "Decision returned")
    @ApiResponse(responseCode = "403", description = "Forbidden — not the job owner")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<EditDecision> checkEditEligibility(
            @Parameter(description = "Job UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.checkEditEligibility(id, clientId, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a job",
               description = "Cancels the job and rejects all active proposals. Only the owning client may cancel.")
    @ApiResponse(responseCode = "204", description = "Job cancelled")
    @ApiResponse(responseCode = "403", description = "Forbidden — not the job owner")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "409", description = "Invalid status transition")
    public ResponseEntity<Void> cancelJob(
            @Parameter(description = "Job UUID") @PathVariable UUID id) {
        UUID clientId = UserContext.getUserId();
        jobService.cancelJob(id, clientId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition job status", description = "Move the job through its lifecycle state machine")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "409", description = "Invalid status transition")
    public ResponseEntity<JobResponse> transitionStatus(
            @Parameter(description = "Job UUID") @PathVariable UUID id,
            @Valid @RequestBody StatusTransitionRequest request) {
        UUID userId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.transitionStatus(id, userId, request.status()));
    }

    @GetMapping("/my-contracts")
    @Operation(summary = "Get jobs where the current freelancer has an accepted proposal")
    @ApiResponse(responseCode = "200", description = "Contracts retrieved")
    public ResponseEntity<PageResponse<JobResponse>> getMyContracts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        UUID freelancerId = UserContext.getUserId();
        var result = jobService.getFreelancerContracts(freelancerId, PageRequest.of(page, size));
        return ResponseEntity.ok(new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        ));
    }

    @PatchMapping("/{id}/submit-delivery")
    @Operation(summary = "Submit work for delivery",
               description = "Freelancer marks the job as completed. Only the accepted freelancer may call this.")
    @ApiResponse(responseCode = "200", description = "Delivery submitted, job moved to COMPLETED")
    @ApiResponse(responseCode = "403", description = "Forbidden — not the accepted freelancer")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "409", description = "Job is not IN_PROGRESS")
    public ResponseEntity<JobResponse> submitDelivery(
            @Parameter(description = "Job UUID") @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        UUID freelancerId = UserContext.getUserId();
        String note = body != null ? body.getOrDefault("note", "") : "";
        return ResponseEntity.ok(jobService.submitDelivery(id, freelancerId, note));
    }
}
