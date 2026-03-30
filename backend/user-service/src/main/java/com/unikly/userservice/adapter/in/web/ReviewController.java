package com.unikly.userservice.adapter.in.web;

import com.unikly.common.dto.PageResponse;
import com.unikly.common.security.UserContext;
import com.unikly.userservice.adapter.in.web.dto.ReviewRequest;
import com.unikly.userservice.adapter.in.web.dto.ReviewResponse;
import com.unikly.userservice.application.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "User reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Create a review", description = "Submit a review for a user after completing a contract")
    @ApiResponse(responseCode = "201", description = "Review created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<ReviewResponse> createReview(
            @Parameter(description = "Reviewed user UUID") @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request) {
        UUID reviewerId = UserContext.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(reviewerId, id, request));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "List reviews for a user")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviews(id, page, size));
    }
}
