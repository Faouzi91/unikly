package com.unikly.store.reviews.api;

import com.unikly.store.reviews.application.ProductReviewService;
import com.unikly.store.reviews.application.ReviewViews.ProductRatingSummaryView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews/summary")
public class ProductRatingSummaryController {
    private final ProductReviewService reviews;

    public ProductRatingSummaryController(ProductReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public List<ProductRatingSummaryView> getPublicRatingSummaries() {
        return reviews.getPublicRatingSummaries();
    }
}
