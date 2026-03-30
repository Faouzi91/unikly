package com.unikly.matchingservice.adapter.in.web;

import com.unikly.matchingservice.adapter.in.web.dto.MatchResultResponse;
import com.unikly.matchingservice.application.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "AI-powered freelancer-job matching")
public class MatchController {

    private final MatchingService matchingService;

    @GetMapping
    @Operation(
            summary = "Get matches",
            description = "Returns ranked match results for a job (by jobId) or for a freelancer (by freelancerId). Exactly one query param must be provided."
    )
    @ApiResponse(responseCode = "200", description = "Matches retrieved")
    @ApiResponse(responseCode = "400", description = "Neither jobId nor freelancerId provided")
    public ResponseEntity<List<MatchResultResponse>> getMatches(
            @Parameter(description = "Job UUID — find matching freelancers") @RequestParam(required = false) UUID jobId,
            @Parameter(description = "Freelancer UUID — find matching jobs") @RequestParam(required = false) UUID freelancerId,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "20") int limit) {

        if (jobId != null) {
            var results = matchingService.getMatchesForJob(jobId, limit).stream()
                    .map(MatchResultResponse::from)
                    .toList();
            return ResponseEntity.ok(results);
        }

        if (freelancerId != null) {
            var results = matchingService.getMatchesForFreelancer(freelancerId, limit).stream()
                    .map(MatchResultResponse::from)
                    .toList();
            return ResponseEntity.ok(results);
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a match result by ID")
    @ApiResponse(responseCode = "200", description = "Match result retrieved")
    @ApiResponse(responseCode = "404", description = "Match not found")
    public ResponseEntity<MatchResultResponse> getMatch(
            @Parameter(description = "Match UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(MatchResultResponse.from(matchingService.getMatch(id)));
    }
}
