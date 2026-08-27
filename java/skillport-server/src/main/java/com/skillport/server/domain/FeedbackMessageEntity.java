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
@Table(name = "feedback_messages", indexes = {
        @Index(name = "idx_feedback_owner_created", columnList = "owner_id,created_at"),
        @Index(name = "idx_feedback_status_created", columnList = "status,created_at"),
        @Index(name = "idx_feedback_public_created", columnList = "created_at,id")
})
public class FeedbackMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(name = "submitter_display_name", nullable = false, length = 120)
    private String submitterDisplayName;
    @Column(nullable = false, length = 32)
    private String kind;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FeedbackMessageEntity() {
    }

    public FeedbackMessageEntity(String publicId, String ownerId, String submitterDisplayName,
                                 String kind, String content, Instant createdAt) {
        this.publicId = publicId;
        this.ownerId = ownerId;
        this.submitterDisplayName = submitterDisplayName;
        this.kind = kind;
        this.content = content;
        this.status = "NEW";
        this.createdAt = createdAt;
    }

    public String getPublicId() { return publicId; }
    public String getOwnerId() { return ownerId; }
    public String getSubmitterDisplayName() { return submitterDisplayName; }
    public String getKind() { return kind; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
