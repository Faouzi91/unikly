package com.unikly.searchservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.searchservice.api.dto.FreelancerSearchResult;
import com.unikly.searchservice.api.dto.JobSearchResult;
import com.unikly.searchservice.application.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text search for jobs and freelancers using Elasticsearch")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/jobs")
    @Operation(summary = "Search jobs", description = "Full-text search for job postings with optional skill and budget filters")
    @ApiResponse(responseCode = "200", description = "Search results retrieved")
    @ApiResponse(responseCode = "503", description = "Elasticsearch unavailable")
    public ResponseEntity<PageResponse<JobSearchResult>> searchJobs(
            @Parameter(description = "Full-text search query") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by required skills") @RequestParam(required = false) List<String> skills,
            @Parameter(description = "Minimum budget") @RequestParam(required = false) Double minBudget,
            @Parameter(description = "Maximum budget") @RequestParam(required = false) Double maxBudget,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(searchService.searchJobs(q, skills, minBudget, maxBudget, page, size));
    }

    @GetMapping("/freelancers")
    @Operation(summary = "Search freelancers", description = "Full-text search for freelancer profiles with optional skill and rating filters")
    @ApiResponse(responseCode = "200", description = "Search results retrieved")
    @ApiResponse(responseCode = "503", description = "Elasticsearch unavailable")
    public ResponseEntity<PageResponse<FreelancerSearchResult>> searchFreelancers(
            @Parameter(description = "Full-text search query") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by skills") @RequestParam(required = false) List<String> skills,
            @Parameter(description = "Minimum rating (1.0–5.0)") @RequestParam(required = false) Double minRating,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(searchService.searchFreelancers(q, skills, minRating, page, size));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Returns autocomplete suggestions for skills or job titles")
    @ApiResponse(responseCode = "200", description = "Suggestions retrieved")
    public ResponseEntity<List<String>> getSuggestions(
            @Parameter(description = "Partial search query") @RequestParam String q,
            @Parameter(description = "Suggestion type: 'skill' or 'title'") @RequestParam(defaultValue = "skill") String type) {

        return ResponseEntity.ok(searchService.getSuggestions(q, type));
    }
}
