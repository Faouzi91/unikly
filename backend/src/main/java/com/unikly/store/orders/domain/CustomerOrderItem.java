package com.unikly.store.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "customer_order_items")
public class CustomerOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;
    @Column(name = "product_id", nullable = false, length = 36) private String productId;
    @Column(name = "product_name", nullable = false, length = 120) private String productName;
    @Column(name = "seller_id", nullable = false) private Long sellerId;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false, length = 24)
    private OrderFulfillmentStatus fulfillmentStatus;
    @Column(name = "carrier_name", length = 80) private String carrierName;
    @Column(name = "tracking_url", length = 2048) private String trackingUrl;

    protected CustomerOrderItem() {}
    public CustomerOrderItem(String productId, String productName, Long sellerId, int quantity, BigDecimal unitPrice) {
        this.productId = productId; this.productName = productName; this.sellerId = sellerId;
        this.quantity = quantity; this.unitPrice = unitPrice;
        this.fulfillmentStatus = OrderFulfillmentStatus.PLACED;
    }
    void setOrder(CustomerOrder order) { this.order = order; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getSellerId() { return sellerId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public OrderFulfillmentStatus getFulfillmentStatus() { return fulfillmentStatus; }
    public String getCarrierName() { return carrierName; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setShippingDetails(String carrierName, String trackingUrl) {
        this.carrierName = carrierName;
        this.trackingUrl = trackingUrl;
    }
    public void advanceFulfillmentStatus(OrderFulfillmentStatus nextStatus) {
        if (fulfillmentStatus.next() != nextStatus) {
            throw new IllegalArgumentException("Fulfillment status must advance one step at a time");
        }
        fulfillmentStatus = nextStatus;
    }
    public void cancel() {
        if (fulfillmentStatus != OrderFulfillmentStatus.PLACED) {
            throw new IllegalStateException("Only placed order items can be canceled");
        }
        fulfillmentStatus = OrderFulfillmentStatus.CANCELED;
    }
}
