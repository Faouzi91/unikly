package com.unikly.store.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "address_line_1", length = 120)
    private String addressLine1;

    @Column(name = "address_line_2", length = 120)
    private String addressLine2;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String region;

    @Column(name = "postal_code", length = 24)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerProfile() {
    }

    public CustomerProfile(Long userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public void update(ContactDetails details) {
        phoneNumber = details.phoneNumber();
        addressLine1 = details.addressLine1();
        addressLine2 = details.addressLine2();
        city = details.city();
        region = details.region();
        postalCode = details.postalCode();
        countryCode = details.countryCode();
        updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
    public Instant getUpdatedAt() { return updatedAt; }

    public record ContactDetails(
            String phoneNumber,
            String addressLine1,
            String addressLine2,
            String city,
            String region,
            String postalCode,
            String countryCode) {
    }
}