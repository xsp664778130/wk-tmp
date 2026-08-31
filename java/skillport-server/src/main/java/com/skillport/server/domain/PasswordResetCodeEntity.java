package com.skillport.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "password_reset_codes", indexes = {
        @Index(name = "idx_password_reset_email_created", columnList = "email_normalized,created_at"),
        @Index(name = "idx_password_reset_expires", columnList = "expires_at")
})
public class PasswordResetCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "email_normalized", nullable = false, length = 254)
    private String emailNormalized;
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;
    @Column(name = "attempts", nullable = false)
    private int attempts;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetCodeEntity() {
    }

    public PasswordResetCodeEntity(String publicId, String emailNormalized, String codeHash,
                                   Instant expiresAt, Instant createdAt) {
        this.publicId = publicId;
        this.emailNormalized = emailNormalized;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void recordFailedAttempt() { attempts += 1; }
    public void consume(Instant now) { consumedAt = now; }
    public String getCodeHash() { return codeHash; }
    public int getAttempts() { return attempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
