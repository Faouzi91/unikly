package com.unikly.userservice.application;

import com.unikly.common.dto.PageResponse;
import com.unikly.userservice.api.dto.ReviewRequest;
import com.unikly.userservice.api.dto.ReviewResponse;
import com.unikly.userservice.api.mapper.ReviewMapper;
import com.unikly.userservice.domain.Review;
import com.unikly.userservice.infrastructure.ReviewRepository;
import com.unikly.userservice.infrastructure.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserProfileRepository profileRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(UUID reviewerId, UUID revieweeId, ReviewRequest request) {
        if (reviewerId.equals(revieweeId)) {
            throw new IllegalArgumentException("You cannot review yourself");
        }

        if (request.jobId() != null && reviewRepository.existsByReviewerIdAndRevieweeIdAndJobId(
                reviewerId, revieweeId, request.jobId())) {
            throw new IllegalStateException("You have already submitted a review for this job");
        }

        var reviewee = profileRepository.findById(revieweeId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found: " + revieweeId));

        var review = Review.builder()
                .reviewerId(reviewerId)
                .revieweeId(revieweeId)
                .jobId(request.jobId())
                .rating(request.rating())
                .comment(request.comment())
                .createdAt(Instant.now())
                .build();

        review = reviewRepository.save(review);

        BigDecimal avg = reviewRepository.calculateAverageRating(revieweeId);
        long count = reviewRepository.countByRevieweeId(revieweeId);
        reviewee.setAverageRating(avg != null ? avg.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        reviewee.setTotalReviews((int) count);
        profileRepository.save(reviewee);

        log.info("Review created: reviewer={}, reviewee={}, rating={}", reviewerId, revieweeId, request.rating());
        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(UUID userId, int page, int size) {
        var result = reviewRepository.findByRevieweeId(userId, PageRequest.of(page, size));
        var content = result.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
