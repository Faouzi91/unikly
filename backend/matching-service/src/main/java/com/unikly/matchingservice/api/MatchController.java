package com.unikly.matchingservice.api;

import com.unikly.matchingservice.api.dto.MatchResultResponse;
import com.unikly.matchingservice.application.MatchingService;
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
public class MatchController {

    private final MatchingService matchingService;

    /**
     * GET /api/v1/matches?jobId={uuid}&limit=20
     * GET /api/v1/matches?freelancerId={uuid}&limit=10
     */
    @GetMapping
    public ResponseEntity<List<MatchResultResponse>> getMatches(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID freelancerId,
            @RequestParam(defaultValue = "20") int limit) {

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

    /** GET /api/v1/matches/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<MatchResultResponse> getMatch(@PathVariable UUID id) {
        return ResponseEntity.ok(MatchResultResponse.from(matchingService.getMatch(id)));
    }
}
