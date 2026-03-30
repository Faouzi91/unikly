package com.unikly.userservice.application.port.out;

import com.unikly.userservice.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByRevieweeId(UUID revieweeId, Pageable pageable);

    boolean existsByReviewerIdAndRevieweeIdAndJobId(UUID reviewerId, UUID revieweeId, UUID jobId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.revieweeId = :revieweeId")
    BigDecimal calculateAverageRating(UUID revieweeId);

    long countByRevieweeId(UUID revieweeId);
}
