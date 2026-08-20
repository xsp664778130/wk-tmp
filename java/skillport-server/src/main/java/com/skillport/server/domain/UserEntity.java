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
@Table(name = "users", indexes = @Index(name = "idx_users_status_created", columnList = "status,created_at"))
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "email_normalized", nullable = false, unique = true, length = 254)
    private String emailNormalized;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(String publicId, String email, String emailNormalized, String displayName,
                      String passwordHash, Instant now) {
        this.publicId = publicId;
        this.email = email;
        this.emailNormalized = emailNormalized;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.status = "ACTIVE";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public String getStatus() { return status; }
}
