package com.unikly.store.reviews.persistence;

import com.unikly.store.reviews.domain.ProductReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findAllByProductIdOrderByCreatedAtDesc(String productId);

    Optional<ProductReview> findByProductIdAndBuyerId(String productId, Long buyerId);

    boolean existsByProductIdAndBuyerId(String productId, Long buyerId);

    long countByProductId(String productId);

    void deleteByProductIdAndBuyerId(String productId, Long buyerId);

    @Query("select r.productId, coalesce(avg(r.rating), 0.0), count(r) from ProductReview r group by r.productId")
    List<Object[]> findAggregateRatings();
}
