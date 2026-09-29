package com.unikly.store.orders.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, unique = true, length = 20) private String reference;
    @Column(name = "buyer_id", nullable = false) private Long buyerId;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 30) private String phone;
    @Column(name = "address_line_1", nullable = false, length = 160) private String addressLine1;
    @Column(name = "address_line_2", length = 160) private String addressLine2;
    @Column(nullable = false, length = 100) private String city;
    @Column(nullable = false, length = 100) private String region;
    @Column(name = "postal_code", nullable = false, length = 24) private String postalCode;
    @Column(nullable = false, length = 80) private String country;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderItem> items = new ArrayList<>();

    protected CustomerOrder() {}

    public CustomerOrder(String id, String reference, Long buyerId, String fullName, String email, String phone,
                         String addressLine1, String addressLine2, String city, String region, String postalCode,
                         String country, BigDecimal total) {
        this.id = id; this.reference = reference; this.buyerId = buyerId; this.status = "PLACED";
        this.fullName = fullName; this.email = email; this.phone = phone; this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2; this.city = city; this.region = region; this.postalCode = postalCode;
        this.country = country; this.total = total; this.createdAt = Instant.now();
    }

    public void addItem(CustomerOrderItem item) { items.add(item); item.setOrder(this); }
    public String getId() { return id; }
    public String getReference() { return reference; }
    public Long getBuyerId() { return buyerId; }
    public String getStatus() { return status; }
    public void updateStatus(String status) { this.status = status; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<CustomerOrderItem> getItems() { return items; }
}
