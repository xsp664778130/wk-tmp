package com.skillport.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pairing_codes", indexes = @Index(name = "idx_pairing_expires", columnList = "expires_at"))
public class PairingCodeEntity {
    @Id
    @Column(name = "code_hash", length = 64)
    private String codeHash;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PairingCodeEntity() {
    }

    public PairingCodeEntity(String codeHash, String ownerId, Instant expiresAt, Instant createdAt) {
        this.codeHash = codeHash;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isUsable(Instant now) { return consumedAt == null && expiresAt.isAfter(now); }
    public void consume(Instant now) { this.consumedAt = now; }
    public String getOwnerId() { return ownerId; }
    public Instant getExpiresAt() { return expiresAt; }
}
