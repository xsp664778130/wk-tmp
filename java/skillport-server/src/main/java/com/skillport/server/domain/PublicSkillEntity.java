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
@Table(name = "public_skills", indexes = {
        @Index(name = "idx_public_skills_published", columnList = "published_at"),
        @Index(name = "idx_public_skills_publisher", columnList = "publisher_owner_id,published_at")
})
public class PublicSkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "source_skill_public_id", nullable = false, unique = true, length = 36)
    private String sourceSkillPublicId;
    @Column(name = "publisher_owner_id", nullable = false, length = 128)
    private String publisherOwnerId;
    @Column(name = "publisher_display_name", nullable = false, length = 120)
    private String publisherDisplayName;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(nullable = false, length = 64)
    private String sha256;
    @Column(name = "tool_compatibility", nullable = false, length = 120)
    private String toolCompatibility;
    @Column(name = "pull_count", nullable = false)
    private long pullCount;
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PublicSkillEntity() {
    }

    public PublicSkillEntity(String publicId, SkillEntity source, String publisherDisplayName, Instant now) {
        this.publicId = publicId;
        this.sourceSkillPublicId = source.getPublicId();
        this.publisherOwnerId = source.getOwnerId();
        this.publisherDisplayName = publisherDisplayName;
        this.name = source.getName();
        this.description = source.getDescription();
        this.category = source.getCategory();
        this.fileName = source.getFileName();
        this.contentType = source.getContentType();
        this.sizeBytes = source.getSizeBytes();
        this.sha256 = source.getSha256();
        this.toolCompatibility = "codex,qoder,openai";
        this.pullCount = 0;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    public void recordPull(Instant now) {
        this.pullCount++;
        this.updatedAt = now;
    }

    public String getPublicId() { return publicId; }
    public String getSourceSkillPublicId() { return sourceSkillPublicId; }
    public String getPublisherOwnerId() { return publisherOwnerId; }
    public String getPublisherDisplayName() { return publisherDisplayName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getToolCompatibility() { return toolCompatibility; }
    public long getPullCount() { return pullCount; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
