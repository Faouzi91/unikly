package com.unikly.store.reviews.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ReviewViews {
    private ReviewViews() {}

    public record ReviewItemView(
            Long id,
            String productId,
            Long buyerId,
            String authorName,
            int rating,
            String title,
            String comment,
            boolean isVerifiedPurchase,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProductReviewSummaryView(
            String productId,
            double averageRating,
            int totalReviews,
            Map<Integer, Long> ratingCounts,
            List<ReviewItemView> reviews,
            boolean currentUserCanReview,
            boolean currentUserVerifiedBuyer,
            ReviewItemView currentUserReview
    ) {}
}
