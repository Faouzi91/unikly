package com.unikly.store.reviews.api;

import com.unikly.store.reviews.application.ProductReviewService;
import com.unikly.store.reviews.application.ReviewRequests;
import com.unikly.store.reviews.application.ReviewViews.ProductReviewSummaryView;
import com.unikly.store.reviews.application.ReviewViews.ReviewItemView;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService reviewService;

    public ProductReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ProductReviewSummaryView getReviews(@PathVariable String productId, Principal principal) {
        String email = principal != null ? principal.getName() : null;
        return reviewService.getProductReviewSummary(productId, email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewItemView submitReview(
            @PathVariable String productId,
            Principal principal,
            @Valid @RequestBody ReviewRequests.SubmitReview request) {
        return reviewService.submitReview(principal.getName(), productId, request);
    }

    @DeleteMapping("/mine")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyReview(@PathVariable String productId, Principal principal) {
        reviewService.deleteMyReview(principal.getName(), productId);
    }
}
