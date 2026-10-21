package com.unikly.store.reviews.application;

import com.unikly.store.catalog.application.CatalogProductService;
import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.application.IdentityAccountService.AccountIdentity;
import com.unikly.store.identity.application.IdentityAccountService.AccountRole;
import com.unikly.store.orders.application.CustomerOrderService;
import com.unikly.store.reviews.application.ReviewViews.ProductReviewSummaryView;
import com.unikly.store.reviews.application.ReviewViews.ReviewItemView;
import com.unikly.store.reviews.application.ReviewViews.ProductRatingSummaryView;
import com.unikly.store.reviews.domain.ProductReview;
import com.unikly.store.reviews.persistence.ProductReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final CatalogProductService catalog;
    private final IdentityAccountService identity;
    private final CustomerOrderService orders;

    public ProductReviewService(
            ProductReviewRepository reviewRepository,
            CatalogProductService catalog,
            IdentityAccountService identity,
            CustomerOrderService orders) {
        this.reviewRepository = reviewRepository;
        this.catalog = catalog;
        this.identity = identity;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public ProductReviewSummaryView getProductReviewSummary(String productId, String currentUserEmail) {
        if (!catalog.exists(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
        }

        List<ProductReview> reviews = reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId);
        int totalReviews = reviews.size();

        double averageRating = 0.0;
        if (totalReviews > 0) {
            double sum = reviews.stream().mapToInt(ProductReview::getRating).sum();
            averageRating = BigDecimal.valueOf(sum / totalReviews)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            int currentStar = star;
            long count = reviews.stream().filter(r -> r.getRating() == currentStar).count();
            ratingCounts.put(currentStar, count);
        }

        List<ReviewItemView> reviewViews = reviews.stream().map(this::toView).toList();

        boolean canReview = false;
        boolean verifiedBuyer = false;
        ReviewItemView currentUserReview = null;

        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            Optional<AccountIdentity> userOpt = identity.findByEmail(currentUserEmail);
            if (userOpt.isPresent()) {
                AccountIdentity user = userOpt.get();
                verifiedBuyer = user.role() == AccountRole.BUYER && orders.hasPurchasedProduct(user.id(), productId);
                canReview = user.role() == AccountRole.BUYER && verifiedBuyer;

                currentUserReview = reviewViews.stream()
                        .filter(r -> r.buyerId().equals(user.id()))
                        .findFirst()
                        .orElse(null);
            }
        }

        return new ProductReviewSummaryView(
                productId,
                averageRating,
                totalReviews,
                ratingCounts,
                reviewViews,
                canReview,
                verifiedBuyer,
                currentUserReview
        );
    }

    @Transactional(readOnly = true)
    public List<ProductRatingSummaryView> getPublicRatingSummaries() {
        List<ProductRatingSummaryView> summaries = new ArrayList<>();
        for (Object[] row : reviewRepository.findAggregateRatings()) {
            String productId = (String) row[0];
            double averageRating = BigDecimal.valueOf(((Number) row[1]).doubleValue())
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
            int totalReviews = ((Number) row[2]).intValue();
            summaries.add(new ProductRatingSummaryView(productId, averageRating, totalReviews));
        }
        return List.copyOf(summaries);
    }

    public ReviewItemView submitReview(String userEmail, String productId, ReviewRequests.SubmitReview request) {
        AccountIdentity user = identity.requireByEmail(userEmail);

        if (user.role() != AccountRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyers can review products");
        }

        if (!catalog.exists(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
        }

        boolean isVerified = orders.hasPurchasedProduct(user.id(), productId);
        if (!isVerified) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only verified purchasers with a delivered order for this product can write a review.");
        }

        String authorName = (user.displayName() != null && !user.displayName().isBlank())
                ? user.displayName().trim()
                : "Customer";

        Optional<ProductReview> existingOpt = reviewRepository.findByProductIdAndBuyerId(productId, user.id());
        ProductReview saved;
        if (existingOpt.isPresent()) {
            ProductReview existing = existingOpt.get();
            existing.update(request.rating(), request.title().trim(), request.comment().trim(), isVerified);
            saved = reviewRepository.save(existing);
        } else {
            ProductReview review = new ProductReview(
                    productId,
                    user.id(),
                    authorName,
                    request.rating(),
                    request.title().trim(),
                    request.comment().trim(),
                    isVerified
            );
            saved = reviewRepository.save(review);
        }

        return toView(saved);
    }

    public void deleteMyReview(String userEmail, String productId) {
        AccountIdentity user = identity.requireByEmail(userEmail);
        if (user.role() != AccountRole.BUYER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyers can delete their reviews");
        }
        reviewRepository.deleteByProductIdAndBuyerId(productId, user.id());
    }

    public void deleteReviewByAdmin(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found: " + reviewId));
        reviewRepository.delete(review);
    }

    private ReviewItemView toView(ProductReview review) {
        return new ReviewItemView(
                review.getId(),
                review.getProductId(),
                review.getBuyerId(),
                review.getAuthorName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
