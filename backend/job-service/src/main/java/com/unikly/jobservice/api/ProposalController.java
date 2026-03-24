package com.unikly.jobservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.api.dto.ProposalResponse;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.application.service.ProposalService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/proposals")
@RequiredArgsConstructor
@Tag(name = "Proposals", description = "Freelancer proposals on job postings")
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping
    @Operation(summary = "Submit a proposal", description = "Submit a proposal for a job. Requires FREELANCER role.")
    @ApiResponse(responseCode = "201", description = "Proposal submitted")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "403", description = "Forbidden — not a freelancer")
    public ResponseEntity<ProposalResponse> submitProposal(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId,
            @Valid @RequestBody SubmitProposalRequest request) {
        UUID freelancerId = UserContext.getUserId();
        UserContext.requireRole("ROLE_FREELANCER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.submitProposal(jobId, freelancerId, request));
    }

    @GetMapping
    @Operation(summary = "List proposals for a job", description = "Returns paginated proposals. Only the owning client may list.")
    @ApiResponse(responseCode = "200", description = "Proposals retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<PageResponse<ProposalResponse>> listProposals(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(proposalService.listProposals(jobId, clientId, page, size));
    }

    @PatchMapping("/{proposalId}/accept")
    @Operation(summary = "Accept a proposal", description = "Accept a freelancer's proposal and create a contract")
    @ApiResponse(responseCode = "200", description = "Proposal accepted")
    @ApiResponse(responseCode = "404", description = "Proposal not found")
    public ResponseEntity<ProposalResponse> acceptProposal(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId,
            @Parameter(description = "Proposal UUID") @PathVariable UUID proposalId) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(proposalService.acceptProposal(jobId, proposalId, clientId));
    }

    @PatchMapping("/{proposalId}/reject")
    @Operation(summary = "Reject a proposal")
    @ApiResponse(responseCode = "200", description = "Proposal rejected")
    @ApiResponse(responseCode = "404", description = "Proposal not found")
    public ResponseEntity<ProposalResponse> rejectProposal(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId,
            @Parameter(description = "Proposal UUID") @PathVariable UUID proposalId) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(proposalService.rejectProposal(jobId, proposalId, clientId));
    }
}
