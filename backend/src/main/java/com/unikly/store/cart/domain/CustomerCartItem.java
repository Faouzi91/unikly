package com.unikly.store.cart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer_cart_items")
public class CustomerCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private CustomerCart cart;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerCartItem() {}

    public CustomerCartItem(CustomerCart cart, String productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public void increaseQuantity(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.quantity += amount;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CustomerCart getCart() {
        return cart;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
