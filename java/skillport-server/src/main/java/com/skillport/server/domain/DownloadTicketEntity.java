package com.skillport.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "download_tickets", indexes = @Index(name = "idx_download_expires", columnList = "expires_at"))
public class DownloadTicketEntity {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(name = "skill_public_id", nullable = false, length = 36)
    private String skillPublicId;
    @Column(name = "device_public_id", nullable = false, length = 36)
    private String devicePublicId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DownloadTicketEntity() {
    }

    public DownloadTicketEntity(String tokenHash, String ownerId, String skillPublicId,
                                String devicePublicId, Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.ownerId = ownerId;
        this.skillPublicId = skillPublicId;
        this.devicePublicId = devicePublicId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
    public String getOwnerId() { return ownerId; }
    public String getSkillPublicId() { return skillPublicId; }
    public String getDevicePublicId() { return devicePublicId; }
    public Instant getExpiresAt() { return expiresAt; }
}
