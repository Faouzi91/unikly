package com.unikly.jobservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.api.dto.ProposalResponse;
import com.unikly.jobservice.api.dto.SubmitProposalRequest;
import com.unikly.jobservice.application.ProposalService;
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
@RequestMapping("/api/jobs/{jobId}/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping
    public ResponseEntity<ProposalResponse> submitProposal(
            @PathVariable UUID jobId,
            @Valid @RequestBody SubmitProposalRequest request) {
        UUID freelancerId = UserContext.getUserId();
        UserContext.requireRole("ROLE_FREELANCER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.submitProposal(jobId, freelancerId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProposalResponse>> listProposals(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(proposalService.listProposals(jobId, clientId, page, size));
    }

    @PatchMapping("/{proposalId}/accept")
    public ResponseEntity<ProposalResponse> acceptProposal(
            @PathVariable UUID jobId,
            @PathVariable UUID proposalId) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(proposalService.acceptProposal(jobId, proposalId, clientId));
    }

    @PatchMapping("/{proposalId}/reject")
    public ResponseEntity<ProposalResponse> rejectProposal(
            @PathVariable UUID jobId,
            @PathVariable UUID proposalId) {
        UUID clientId = UserContext.getUserId();
        return ResponseEntity.ok(proposalService.rejectProposal(jobId, proposalId, clientId));
    }
}
