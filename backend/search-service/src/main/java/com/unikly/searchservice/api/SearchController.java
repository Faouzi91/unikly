package com.unikly.searchservice.api;

import com.unikly.common.dto.PageResponse;
import com.unikly.searchservice.api.dto.FreelancerSearchResult;
import com.unikly.searchservice.api.dto.JobSearchResult;
import com.unikly.searchservice.application.SearchService;
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
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/jobs")
    public ResponseEntity<PageResponse<JobSearchResult>> searchJobs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(searchService.searchJobs(q, skills, minBudget, maxBudget, page, size));
    }

    @GetMapping("/freelancers")
    public ResponseEntity<PageResponse<FreelancerSearchResult>> searchFreelancers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(searchService.searchFreelancers(q, skills, minRating, page, size));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "skill") String type) {

        return ResponseEntity.ok(searchService.getSuggestions(q, type));
    }
}
