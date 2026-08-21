package com.skillport.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "skills", indexes = {
        @Index(name = "idx_skills_owner_created", columnList = "owner_id,created_at"),
        @Index(name = "idx_skills_owner_source", columnList = "owner_id,source_public_skill_id", unique = true)
})
public class SkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, length = 64)
    private String category;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;
    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(nullable = false, length = 64)
    private String sha256;
    @Column(nullable = false, length = 2000)
    private String note;
    @Column(name = "source_public_skill_id", length = 36)
    private String sourcePublicSkillId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillEntity() {
    }

    public SkillEntity(String publicId, String ownerId, String name, String description, String category,
                       String fileName, String storagePath, String contentType, long sizeBytes, String sha256,
                       Instant createdAt) {
        this(publicId, ownerId, name, description, category, fileName, storagePath, contentType, sizeBytes,
                sha256, null, createdAt);
    }

    public SkillEntity(String publicId, String ownerId, String name, String description, String category,
                       String fileName, String storagePath, String contentType, long sizeBytes, String sha256,
                       String sourcePublicSkillId, Instant createdAt) {
        this.publicId = publicId;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.note = "";
        this.sourcePublicSkillId = sourcePublicSkillId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void updateNote(String note, Instant now) {
        this.note = note;
        this.updatedAt = now;
    }

    public String getPublicId() { return publicId; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getFileName() { return fileName; }
    public String getStoragePath() { return storagePath; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getNote() { return note; }
    public String getSourcePublicSkillId() { return sourcePublicSkillId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
