package com.skillport.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "devices", indexes = @Index(name = "idx_devices_owner_seen", columnList = "owner_id,last_seen_at"))
public class DeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 32)
    private String os;
    @Column(nullable = false, length = 32)
    private String arch;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeviceEntity() {
    }

    public DeviceEntity(String publicId, String ownerId, String name, String os, String arch,
                        String tokenHash, Instant createdAt) {
        this.publicId = publicId;
        this.ownerId = ownerId;
        this.name = name;
        this.os = os;
        this.arch = arch;
        this.tokenHash = tokenHash;
        this.status = "OFFLINE";
        this.createdAt = createdAt;
    }

    public void markOnline(Instant now) { this.status = "ONLINE"; this.lastSeenAt = now; }
    public void markOffline(Instant now) { this.status = "OFFLINE"; this.lastSeenAt = now; }

    public String getPublicId() { return publicId; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getOs() { return os; }
    public String getArch() { return arch; }
    public String getTokenHash() { return tokenHash; }
    public String getStatus() { return status; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
}
