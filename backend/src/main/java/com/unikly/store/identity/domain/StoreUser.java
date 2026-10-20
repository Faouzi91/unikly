package com.unikly.store.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "store_users", uniqueConstraints = @UniqueConstraint(name = "uk_store_users_email", columnNames = "email"))
public class StoreUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreRole role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StoreUser() {
    }

    public StoreUser(String email, String passwordHash, String displayName, StoreRole role) {
        this(email, passwordHash, displayName, role, true);
    }

    public StoreUser(String email, String passwordHash, String displayName, StoreRole role, boolean emailVerified) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.emailVerified = emailVerified;
        this.createdAt = Instant.now();
    }

    public static StoreUser unverified(String email, String passwordHash, String displayName, StoreRole role) {
        return new StoreUser(email, passwordHash, displayName, role, false);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StoreRole getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateRole(StoreRole role) {
        this.role = role;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }
}
