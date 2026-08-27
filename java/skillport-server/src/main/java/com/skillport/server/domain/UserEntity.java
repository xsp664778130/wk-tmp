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
@Table(name = "users", indexes = {
        @Index(name = "idx_users_status_created", columnList = "status,created_at"),
        @Index(name = "uk_users_wecom_identity", columnList = "wecom_corp_id,wecom_user_id", unique = true)
})
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
    @Column(name = "wecom_corp_id", length = 64)
    private String weComCorpId;
    @Column(name = "wecom_user_id", length = 128)
    private String weComUserId;
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
        this(publicId, email, emailNormalized, displayName, passwordHash, null, null, now);
    }

    public UserEntity(String publicId, String email, String emailNormalized, String displayName,
                      String passwordHash, String weComCorpId, String weComUserId, Instant now) {
        this.publicId = publicId;
        this.email = email;
        this.emailNormalized = emailNormalized;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.weComCorpId = weComCorpId;
        this.weComUserId = weComUserId;
        this.status = "ACTIVE";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public String getWeComCorpId() { return weComCorpId; }
    public String getWeComUserId() { return weComUserId; }
    public String getStatus() { return status; }
}
