package com.skillport.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "device_local_skills",
        indexes = @Index(name = "idx_local_skills_owner_device", columnList = "owner_id,device_public_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_device_local_skill",
                columnNames = {"device_public_id", "tool", "slug"}))
public class DeviceLocalSkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(name = "device_public_id", nullable = false, length = 36)
    private String devicePublicId;
    @Column(nullable = false, length = 32)
    private String tool;
    @Column(nullable = false, length = 180)
    private String slug;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(name = "relative_path", nullable = false, length = 512)
    private String relativePath;
    @Column(name = "origin_skill_id", length = 64)
    private String originSkillId;
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected DeviceLocalSkillEntity() {
    }

    public DeviceLocalSkillEntity(String ownerId, String devicePublicId, String tool, String slug,
                                  String name, String description, String relativePath,
                                  String originSkillId, Instant detectedAt) {
        this.ownerId = ownerId;
        this.devicePublicId = devicePublicId;
        this.tool = tool;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.relativePath = relativePath;
        this.originSkillId = originSkillId;
        this.detectedAt = detectedAt;
    }

    public String getOwnerId() { return ownerId; }
    public String getDevicePublicId() { return devicePublicId; }
    public String getTool() { return tool; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRelativePath() { return relativePath; }
    public String getOriginSkillId() { return originSkillId; }
    public Instant getDetectedAt() { return detectedAt; }
}
