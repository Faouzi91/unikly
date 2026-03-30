package com.unikly.jobservice.invitation.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unikly.common.dto.PageResponse;
import com.unikly.common.events.FreelancerInvitedEvent;
import com.unikly.common.outbox.OutboxEvent;
import com.unikly.common.outbox.OutboxRepository;
import com.unikly.common.security.UserContext;
import com.unikly.jobservice.invitation.adapter.in.web.dto.InvitationResponse;
import com.unikly.jobservice.invitation.adapter.in.web.dto.InviteFreelancerRequest;
import com.unikly.jobservice.invitation.domain.model.Invitation;
import com.unikly.jobservice.job.domain.model.Job;
import com.unikly.jobservice.job.domain.model.JobStatus;
import com.unikly.jobservice.invitation.application.port.out.InvitationRepository;
import com.unikly.jobservice.job.application.port.out.JobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Invite freelancers to apply for jobs")
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final JobRepository jobRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/{jobId}/invitations")
    @Operation(summary = "Invite a freelancer to a job",
               description = "Creates an invitation and sends a notification to the freelancer. Job must be OPEN and caller must be the owner.")
    @ApiResponse(responseCode = "201", description = "Invitation created")
    @ApiResponse(responseCode = "400", description = "Freelancer already invited")
    @ApiResponse(responseCode = "403", description = "Forbidden — not the job owner")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "409", description = "Job is not OPEN")
    public ResponseEntity<InvitationResponse> inviteFreelancer(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId,
            @Valid @RequestBody InviteFreelancerRequest request) {

        UUID clientId = UserContext.getUserId();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found: " + jobId));

        if (!job.getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the job owner can send invitations");
        }
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job must be OPEN to send invitations");
        }
        if (invitationRepository.existsByJobIdAndFreelancerId(jobId, request.freelancerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Freelancer has already been invited to this job");
        }

        var invitation = Invitation.builder()
                .jobId(jobId)
                .clientId(clientId)
                .freelancerId(request.freelancerId())
                .message(request.message())
                .build();
        invitation = invitationRepository.save(invitation);

        publishFreelancerInvitedEvent(invitation, job.getTitle());

        log.info("Client {} invited freelancer {} to job {}", clientId, request.freelancerId(), jobId);
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationResponse.from(invitation));
    }

    @GetMapping("/{jobId}/invitations")
    @Operation(summary = "List invitations for a job")
    @ApiResponse(responseCode = "200", description = "Invitations retrieved")
    public ResponseEntity<PageResponse<InvitationResponse>> getJobInvitations(
            @Parameter(description = "Job UUID") @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var result = invitationRepository.findByJobId(jobId, PageRequest.of(page, size));
        var content = result.getContent().stream().map(InvitationResponse::from).toList();
        return ResponseEntity.ok(new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/invitations/mine")
    @Operation(summary = "List invitations received by the current freelancer")
    @ApiResponse(responseCode = "200", description = "Invitations retrieved")
    public ResponseEntity<PageResponse<InvitationResponse>> getMyInvitations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID freelancerId = UserContext.getUserId();
        var result = invitationRepository.findByFreelancerId(freelancerId, PageRequest.of(page, size));
        var content = result.getContent().stream().map(InvitationResponse::from).toList();
        return ResponseEntity.ok(new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    private void publishFreelancerInvitedEvent(Invitation invitation, String jobTitle) {
        try {
            var event = new FreelancerInvitedEvent(
                    UUID.randomUUID(), null, Instant.now(),
                    invitation.getJobId(), invitation.getClientId(),
                    invitation.getFreelancerId(), jobTitle,
                    invitation.getMessage()
            );
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(event.eventType(), invitation.getJobId(), "Invitation", payload));
        } catch (Exception e) {
            log.error("Failed to serialize FreelancerInvitedEvent", e);
        }
    }
}
