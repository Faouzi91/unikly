package com.unikly.store.cart.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "customer_carts")
public class CustomerCart {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "buyer_id", nullable = false, unique = true)
    private Long buyerId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerCartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerCart() {}

    public CustomerCart(String id, Long buyerId) {
        this.id = id;
        this.buyerId = buyerId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Optional<CustomerCartItem> findItem(String productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    public CustomerCartItem addItem(String productId, int quantity) {
        Optional<CustomerCartItem> existing = findItem(productId);
        if (existing.isPresent()) {
            CustomerCartItem item = existing.get();
            item.increaseQuantity(quantity);
            this.updatedAt = Instant.now();
            return item;
        } else {
            CustomerCartItem item = new CustomerCartItem(this, productId, quantity);
            this.items.add(item);
            this.updatedAt = Instant.now();
            return item;
        }
    }

    public boolean updateItem(String productId, int quantity) {
        Optional<CustomerCartItem> existing = findItem(productId);
        if (existing.isPresent()) {
            existing.get().updateQuantity(quantity);
            this.updatedAt = Instant.now();
            return true;
        }
        return false;
    }

    public boolean removeItem(String productId) {
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (removed) {
            this.updatedAt = Instant.now();
        }
        return removed;
    }

    public void clear() {
        this.items.clear();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public List<CustomerCartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
