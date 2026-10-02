package com.unikly.store.reviews.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_reviews")
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Column(name = "is_verified_purchase", nullable = false)
    private boolean isVerifiedPurchase;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductReview() {}

    public ProductReview(String productId, Long buyerId, String authorName, int rating,
                         String title, String comment, boolean isVerifiedPurchase) {
        validateRating(rating);
        this.productId = productId;
        this.buyerId = buyerId;
        this.authorName = authorName;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.isVerifiedPurchase = isVerifiedPurchase;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(int rating, String title, String comment, boolean isVerifiedPurchase) {
        validateRating(rating);
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        if (isVerifiedPurchase) {
            this.isVerifiedPurchase = true;
        }
        this.updatedAt = Instant.now();
    }

    private static void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public Long getBuyerId() { return buyerId; }
    public String getAuthorName() { return authorName; }
    public int getRating() { return rating; }
    public String getTitle() { return title; }
    public String getComment() { return comment; }
    public boolean isVerifiedPurchase() { return isVerifiedPurchase; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
