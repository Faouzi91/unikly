package com.unikly.jobservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.api.dto.CreateJobRequest;
import com.unikly.jobservice.api.dto.JobResponse;
import com.unikly.jobservice.api.dto.StatusTransitionRequest;
import com.unikly.jobservice.api.dto.UpdateJobRequest;
import com.unikly.jobservice.application.service.JobService;
import com.unikly.jobservice.domain.JobStatus;
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

import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
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

/*
    @GetMapping
    @Operation(summary = "List jobs", description = "Returns a paginated list of jobs with optional filters")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved")
    public ResponseEntity<PageResponse<JobResponse>> listJobs(
            @Parameter(description = "Filter by job status") @RequestParam(required = false) JobStatus status,
            @Parameter(description = "Filter by required skill") @RequestParam(required = false) String skill,
            @Parameter(description = "Minimum budget") @RequestParam(required = false) BigDecimal minBudget,
            @Parameter(description = "Maximum budget") @RequestParam(required = false) BigDecimal maxBudget,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobService.listJobs(status, skill, minBudget, maxBudget, sort, direction, page, size));
    }
*/

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

/*
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

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get total active jobs for admin dashboard")
    public ResponseEntity<Map<String, Long>> getAdminStats() {
        return ResponseEntity.ok(Map.of("totalActiveJobs", jobService.getTotalActiveJobs()));
    }

    @PatchMapping("/admin/{id}/close")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Admin force-close an inappropriate job")
    public ResponseEntity<JobResponse> adminCloseJob(@PathVariable UUID id) {
        UUID adminId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.adminCloseJob(id, adminId));
    }

    @PatchMapping("/{id}/submit-delivery")
    @Operation(summary = "Submit work for delivery (freelancer)")
    @ApiResponse(responseCode = "200", description = "Delivery submitted")
    @ApiResponse(responseCode = "403", description = "Only the assigned freelancer may submit delivery")
    public ResponseEntity<JobResponse> submitDelivery(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        UUID freelancerId = UserContext.getUserId();
        String note = body != null ? body.getOrDefault("note", "") : "";
        return ResponseEntity.ok(jobService.submitDelivery(id, freelancerId, note));
    }

    @GetMapping("/my-contracts")
    @Operation(summary = "Get active contracts for the current freelancer")
    @ApiResponse(responseCode = "200", description = "Contracts retrieved")
    public ResponseEntity<PageResponse<JobResponse>> getMyContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID freelancerId = UserContext.getUserId();
        return ResponseEntity.ok(jobService.getFreelancerContracts(freelancerId, page, size));
    }
*/
}
