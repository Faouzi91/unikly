package com.unikly.store.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "seller_store_profiles")
public class SellerStoreProfile {
    @Id
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "store_name", nullable = false, length = 80)
    private String storeName;

    @Column(length = 140)
    private String headline;

    @Column(length = 600)
    private String description;

    @Column(length = 80)
    private String location;

    @Column(name = "support_email", length = 160)
    private String supportEmail;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SellerStoreProfile() {}

    public SellerStoreProfile(Long sellerId, String storeName, String headline,
                              String description, String location, String supportEmail) {
        this.sellerId = sellerId;
        this.storeName = storeName;
        this.headline = headline;
        this.description = description;
        this.location = location;
        this.supportEmail = supportEmail;
        this.updatedAt = Instant.now();
    }

    public void update(String storeName, String headline, String description,
                       String location, String supportEmail) {
        this.storeName = storeName;
        this.headline = headline;
        this.description = description;
        this.location = location;
        this.supportEmail = supportEmail;
        this.updatedAt = Instant.now();
    }

    public Long getSellerId() { return sellerId; }
    public String getStoreName() { return storeName; }
    public String getHeadline() { return headline; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getSupportEmail() { return supportEmail; }
    public Instant getUpdatedAt() { return updatedAt; }
}
