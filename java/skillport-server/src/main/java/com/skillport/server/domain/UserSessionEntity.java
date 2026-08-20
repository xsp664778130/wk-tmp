package com.skillport.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_user_sessions_owner_created", columnList = "owner_id,created_at"),
        @Index(name = "idx_user_sessions_expires", columnList = "expires_at")
})
public class UserSessionEntity {
    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserSessionEntity() {
    }

    public UserSessionEntity(String tokenHash, String ownerId, Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public String getOwnerId() { return ownerId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
