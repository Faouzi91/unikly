package com.unikly.store.reviews.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ReviewRequests {
    private ReviewRequests() {}

    public record SubmitReview(
            @Min(value = 1, message = "Rating must be at least 1 star")
            @Max(value = 5, message = "Rating cannot exceed 5 stars")
            int rating,

            @NotBlank(message = "Review title is required")
            @Size(max = 120, message = "Review title cannot exceed 120 characters")
            String title,

            @NotBlank(message = "Review comment is required")
            @Size(max = 2000, message = "Review comment cannot exceed 2000 characters")
            String comment
    ) {}
}
