package com.unikly.store.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "catalog_products")
public class CatalogProduct {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "seller_id")
    private Long sellerId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, length = 2000)
    private String image;
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogProduct() {}

    public CatalogProduct(String id, Long sellerId, String name, String description, String category,
                          BigDecimal price, String image, int stockQuantity) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.image = image;
        this.stockQuantity = stockQuantity;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String description, String category, BigDecimal price, String image, int stockQuantity) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.image = image;
        this.stockQuantity = stockQuantity;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public Long getSellerId() { return sellerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public String getImage() { return image; }
    public int getStockQuantity() { return stockQuantity; }
    public void decreaseStock(int quantity) {
        if (quantity < 1 || quantity > stockQuantity) throw new IllegalArgumentException("Insufficient stock");
        stockQuantity -= quantity;
        updatedAt = Instant.now();
    }
    public void increaseStock(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Restocked quantity must be positive");
        stockQuantity += quantity;
        updatedAt = Instant.now();
    }
}
