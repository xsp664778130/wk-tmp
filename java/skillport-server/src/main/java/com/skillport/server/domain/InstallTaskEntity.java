package com.skillport.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "install_tasks", indexes = {
        @Index(name = "idx_tasks_owner_created", columnList = "owner_id,created_at"),
        @Index(name = "idx_tasks_device_status", columnList = "device_public_id,status")
})
public class InstallTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;
    @Column(name = "skill_public_id", nullable = false, length = 36)
    private String skillPublicId;
    @Column(name = "device_public_id", nullable = false, length = 36)
    private String devicePublicId;
    @Column(nullable = false, length = 255)
    private String targets;
    @Column(nullable = false, length = 16)
    private String operation;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(nullable = false)
    private int progress;
    @Column(name = "stage", nullable = false, length = 64)
    private String stage;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstallTaskEntity() {
    }

    public InstallTaskEntity(String publicId, String ownerId, String skillPublicId, String devicePublicId,
                             String targets, Instant now) {
        this(publicId, ownerId, skillPublicId, devicePublicId, targets, "INSTALL", now);
    }

    public InstallTaskEntity(String publicId, String ownerId, String skillPublicId, String devicePublicId,
                             String targets, String operation, Instant now) {
        this.publicId = publicId;
        this.ownerId = ownerId;
        this.skillPublicId = skillPublicId;
        this.devicePublicId = devicePublicId;
        this.targets = targets;
        this.operation = operation;
        this.status = "PENDING";
        this.progress = 0;
        this.stage = "QUEUED";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markSent(Instant now) { this.status = "RUNNING"; this.stage = "SENT"; this.updatedAt = now; }
    public void updateProgress(int progress, String stage, Instant now) {
        this.progress = Math.max(this.progress, Math.max(0, Math.min(100, progress)));
        this.stage = stage;
        this.status = this.progress >= 100 ? "COMPLETED" : "RUNNING";
        this.updatedAt = now;
    }
    public void fail(String message, Instant now) {
        this.status = "FAILED";
        this.stage = "FAILED";
        this.errorMessage = message == null ? ("UNINSTALL".equals(operation) ? "卸载失败" : "安装失败")
                : message.substring(0, Math.min(1000, message.length()));
        this.updatedAt = now;
    }

    public String getPublicId() { return publicId; }
    public String getOwnerId() { return ownerId; }
    public String getSkillPublicId() { return skillPublicId; }
    public String getDevicePublicId() { return devicePublicId; }
    public String getTargets() { return targets; }
    public String getOperation() { return operation; }
    public String getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getStage() { return stage; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
