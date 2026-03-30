package com.unikly.jobservice.adapter.in.web;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.domain.model.Contract;
import com.unikly.jobservice.domain.model.ContractStatus;
import com.unikly.jobservice.application.port.out.ContractRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Contracts", description = "Contract management")
public class ContractController {

    private final ContractRepository contractRepository;

    @GetMapping("/mine")
    @Operation(summary = "Get contracts for the current user (as client or freelancer)")
    @ApiResponse(responseCode = "200", description = "Contracts retrieved")
    public ResponseEntity<PageResponse<ContractResponse>> getMyContracts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = UserContext.getUserId();
        var result = contractRepository.findByParticipant(userId, PageRequest.of(page, size));
        var content = result.getContent().stream().map(ContractResponse::from).toList();
        return ResponseEntity.ok(new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get the contract for a specific job")
    @ApiResponse(responseCode = "200", description = "Contract retrieved")
    @ApiResponse(responseCode = "404", description = "No contract for this job")
    public ResponseEntity<ContractResponse> getContractByJobId(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId) {

        var contract = contractRepository.findByJobId(jobId)
                .orElseThrow(() -> new EntityNotFoundException("No contract found for job: " + jobId));
        return ResponseEntity.ok(ContractResponse.from(contract));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a contract as completed",
               description = "Only the client can mark a contract as completed. Sets completedAt and transitions status to COMPLETED.")
    @ApiResponse(responseCode = "200", description = "Contract completed")
    @ApiResponse(responseCode = "403", description = "Forbidden — not the client")
    @ApiResponse(responseCode = "409", description = "Contract is not ACTIVE")
    public ResponseEntity<ContractResponse> completeContract(
            @Parameter(description = "Contract UUID") @PathVariable UUID id) {

        UUID userId = UserContext.getUserId();
        var contract = contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + id));

        if (!contract.getClientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the client can complete a contract");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contract is not ACTIVE");
        }

        contract.setStatus(ContractStatus.COMPLETED);
        contract.setCompletedAt(Instant.now());
        contract = contractRepository.save(contract);

        log.info("Contract {} completed by client {}", id, userId);
        return ResponseEntity.ok(ContractResponse.from(contract));
    }

    // ── Inline DTO ─────────────────────────────────────────────────────────

    public record ContractResponse(
            UUID id,
            UUID jobId,
            UUID clientId,
            UUID freelancerId,
            java.math.BigDecimal agreedBudget,
            String terms,
            ContractStatus status,
            Instant startedAt,
            Instant completedAt
    ) {
        public static ContractResponse from(Contract c) {
            return new ContractResponse(
                    c.getId(), c.getJobId(), c.getClientId(), c.getFreelancerId(),
                    c.getAgreedBudget(), c.getTerms(), c.getStatus(),
                    c.getStartedAt(), c.getCompletedAt()
            );
        }
    }
}
