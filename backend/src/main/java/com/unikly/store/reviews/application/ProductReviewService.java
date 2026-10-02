package com.unikly.store.reviews.application;

import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.orders.persistence.CustomerOrderRepository;
import com.unikly.store.reviews.application.ReviewViews.ProductReviewSummaryView;
import com.unikly.store.reviews.application.ReviewViews.ReviewItemView;
import com.unikly.store.reviews.domain.ProductReview;
import com.unikly.store.reviews.persistence.ProductReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final CatalogProductRepository productRepository;
    private final StoreUserRepository userRepository;
    private final CustomerOrderRepository orderRepository;

    public ProductReviewService(
            ProductReviewRepository reviewRepository,
            CatalogProductRepository productRepository,
            StoreUserRepository userRepository,
            CustomerOrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public ProductReviewSummaryView getProductReviewSummary(String productId, String currentUserEmail) {
        if (!productRepository.existsById(productId)) {
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
            Optional<StoreUser> userOpt = userRepository.findByEmail(currentUserEmail);
            if (userOpt.isPresent()) {
                StoreUser user = userOpt.get();
                verifiedBuyer = orderRepository.hasPurchasedProduct(user.getId(), productId);
                canReview = (user.getRole() == StoreRole.ADMIN) ||
                        (user.getRole() == StoreRole.BUYER && verifiedBuyer);

                currentUserReview = reviewViews.stream()
                        .filter(r -> r.buyerId().equals(user.getId()))
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

    public ReviewItemView submitReview(String userEmail, String productId, ReviewRequests.SubmitReview request) {
        StoreUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

        if (user.getRole() != StoreRole.BUYER && user.getRole() != StoreRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only buyers can review products");
        }

        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId);
        }

        boolean isVerified = orderRepository.hasPurchasedProduct(user.getId(), productId);
        if (!isVerified && user.getRole() != StoreRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only verified purchasers who have ordered this product can write a review.");
        }

        String authorName = (user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName().trim()
                : "Customer";

        Optional<ProductReview> existingOpt = reviewRepository.findByProductIdAndBuyerId(productId, user.getId());
        ProductReview saved;
        if (existingOpt.isPresent()) {
            ProductReview existing = existingOpt.get();
            existing.update(request.rating(), request.title().trim(), request.comment().trim(), isVerified);
            saved = reviewRepository.save(existing);
        } else {
            ProductReview review = new ProductReview(
                    productId,
                    user.getId(),
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
        StoreUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        reviewRepository.deleteByProductIdAndBuyerId(productId, user.getId());
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
