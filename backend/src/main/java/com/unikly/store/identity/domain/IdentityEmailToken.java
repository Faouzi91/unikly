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
@Table(name = "identity_email_tokens", uniqueConstraints =
        @UniqueConstraint(name = "uk_identity_email_tokens_hash", columnNames = "token_hash"))
public class IdentityEmailToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdentityEmailPurpose purpose;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected IdentityEmailToken() {
    }

    public IdentityEmailToken(
            Long userId,
            String tokenHash,
            IdentityEmailPurpose purpose,
            Instant createdAt,
            Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }

    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public IdentityEmailPurpose getPurpose() { return purpose; }
    public Instant getExpiresAt() { return expiresAt; }
}
