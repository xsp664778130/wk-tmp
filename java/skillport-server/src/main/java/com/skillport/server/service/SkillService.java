package com.skillport.server.service;

import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.SkillRepository;
import com.skillport.server.repository.PublicSkillRepository;
import com.skillport.server.storage.FileStorageService;
import com.skillport.server.storage.StoredSkillFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SkillService {
    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;
    private static final int MAX_USAGE_STEPS = 20;
    private static final String DEFAULT_CATEGORY = "编程技能";
    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "编程技能", "测试技能", "排查技能", "日志技能");
    private final SkillRepository skillRepository;
    private final PublicSkillRepository publicSkillRepository;
    private final FileStorageService fileStorageService;
    private final SkillPackageValidator packageValidator;

    public SkillService(SkillRepository skillRepository, PublicSkillRepository publicSkillRepository,
                        FileStorageService fileStorageService, SkillPackageValidator packageValidator) {
        this.skillRepository = skillRepository;
        this.publicSkillRepository = publicSkillRepository;
        this.fileStorageService = fileStorageService;
        this.packageValidator = packageValidator;
    }

    @Transactional(readOnly = true)
    public List<SkillEntity> list(String ownerId) {
        return skillRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional
    public SkillEntity upload(String ownerId, String name, String description, String detail,
                              String usageSteps, String category,
                              MultipartFile file, MultipartFile avatar) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill 文件为空或超过 25MB");
        }
        SkillPackageValidator.SkillPackageMetadata metadata = packageValidator.validate(file);
        AvatarUpload avatarUpload = readAvatar(avatar);
        String publicId = UUID.randomUUID().toString();
        try {
            StoredSkillFile stored = fileStorageService.store(ownerId, publicId, file.getOriginalFilename(), file.getInputStream());
            Instant now = Instant.now();
            String originalFilename = file.getOriginalFilename() == null ? "skill.zip" : file.getOriginalFilename();
            SkillEntity skill = new SkillEntity(
                    publicId, ownerId, safeText(name, 160, metadata.displayName()),
                    safeText(description, 2000, metadata.description()),
                    normalizeCategory(category), originalFilename, stored.path().toString(),
                    safeText(file.getContentType(), 120, "application/octet-stream"), stored.sizeBytes(), stored.sha256(), now);
            String normalizedUsageSteps = normalizeUsageSteps(usageSteps);
            if (detail != null && !detail.isBlank() && normalizedUsageSteps.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少填写一个 Skill 使用步骤");
            }
            skill.initializeDetails(safeText(detail, 10000, skill.getDescription()), normalizedUsageSteps);
            if (avatarUpload != null) {
                StoredSkillFile storedAvatar = fileStorageService.store(ownerId, publicId,
                        "__skillport_avatar_" + publicId + "." + avatarUpload.extension(),
                        new ByteArrayInputStream(avatarUpload.bytes()));
                skill.attachAvatar(storedAvatar.path().getFileName().toString(), storedAvatar.path().toString(),
                        avatarUpload.contentType(), storedAvatar.sizeBytes(), storedAvatar.sha256());
            }
            return skillRepository.save(skill);
        } catch (IOException | RuntimeException exception) {
            deleteFilesQuietly(fileStoragePath(ownerId, publicId));
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("无法读取上传文件", exception);
        }
    }

    public SkillEntity upload(String ownerId, String name, String description, String category, MultipartFile file) {
        return upload(ownerId, name, description, "", "", category, file, null);
    }

    public SkillEntity upload(String ownerId, String name, String description, String category,
                              MultipartFile file, MultipartFile avatar) {
        return upload(ownerId, name, description, "", "", category, file, avatar);
    }

    @Transactional
    public SkillEntity updateNote(String ownerId, String publicId, String note) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        skill.updateNote(safeText(note, 2000), Instant.now());
        return skill;
    }

    @Transactional
    public CategoryUpdateResult updateCategory(String ownerId, String publicId, String category) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        String normalizedCategory = requireCategory(category);
        Instant now = Instant.now();
        skill.updateCategory(normalizedCategory, now);
        boolean publicPoolSynchronized = publicSkillRepository
                .findBySourceSkillPublicIdAndPublisherOwnerId(publicId, ownerId)
                .map(publication -> {
                    publication.updateCategory(normalizedCategory, now);
                    return true;
                })
                .orElse(false);
        return new CategoryUpdateResult(skill, publicPoolSynchronized);
    }

    @Transactional
    public DetailUpdateResult updateDetails(String ownerId, String publicId, String name, String description,
                                            String detail, List<String> usageSteps) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        String normalizedName = requiredText(name, 160, "请填写 Skill 名称");
        String normalizedDescription = requiredText(description, 2000, "请填写 Skill 描述");
        String normalizedDetail = requiredText(detail, 10000, "请填写 Skill 详细说明");
        String normalizedUsageSteps = normalizeUsageSteps(usageSteps);
        if (normalizedUsageSteps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少填写一个 Skill 使用步骤");
        }
        Instant now = Instant.now();
        skill.updateDetails(normalizedName, normalizedDescription, normalizedDetail, normalizedUsageSteps, now);
        boolean publicPoolSynchronized = publicSkillRepository
                .findBySourceSkillPublicIdAndPublisherOwnerId(publicId, ownerId)
                .map(publication -> {
                    publication.updateDetails(normalizedName, normalizedDescription, normalizedDetail,
                            normalizedUsageSteps, now);
                    return true;
                })
                .orElse(false);
        return new DetailUpdateResult(skill, publicPoolSynchronized);
    }

    @Transactional
    public AvatarUpdateResult updateAvatar(String ownerId, String publicId, MultipartFile avatar) {
        AvatarUpload avatarUpload = readAvatar(avatar);
        if (avatarUpload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择 Skill 头像");
        }
        SkillEntity skill = ownedSkill(ownerId, publicId);
        String previousAvatarPath = skill.getAvatarStoragePath();
        StoredSkillFile storedAvatar = fileStorageService.store(ownerId, publicId,
                "__skillport_avatar_" + UUID.randomUUID() + "." + avatarUpload.extension(),
                new ByteArrayInputStream(avatarUpload.bytes()));
        skill.updateAvatar(storedAvatar.path().getFileName().toString(), storedAvatar.path().toString(),
                avatarUpload.contentType(), storedAvatar.sizeBytes(), storedAvatar.sha256(), Instant.now());
        cleanUpAvatarAfterTransaction(previousAvatarPath, storedAvatar.path().toString());
        return new AvatarUpdateResult(skill, isShared(ownerId, publicId));
    }

    @Transactional
    public AvatarUpdateResult removeAvatar(String ownerId, String publicId) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        String previousAvatarPath = skill.getAvatarStoragePath();
        if (previousAvatarPath != null && !previousAvatarPath.isBlank()) {
            skill.removeAvatar(Instant.now());
            deleteFileAfterCommit(previousAvatarPath);
        }
        return new AvatarUpdateResult(skill, isShared(ownerId, publicId));
    }

    @Transactional(readOnly = true)
    public SkillEntity ownedSkill(String ownerId, String publicId) {
        return skillRepository.findByPublicIdAndOwnerId(publicId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill 不存在"));
    }

    @Transactional(readOnly = true)
    public Path ownedFile(String ownerId, String publicId) {
        return fileStorageService.resolve(ownedSkill(ownerId, publicId).getStoragePath());
    }

    @Transactional(readOnly = true)
    public Path ownedAvatar(String ownerId, String publicId) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        if (!skill.hasAvatar()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill 头像不存在");
        }
        return fileStorageService.resolve(skill.getAvatarStoragePath());
    }

    @Transactional
    public void deleteOwned(String ownerId, String publicId) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        publicSkillRepository.findBySourceSkillPublicIdAndPublisherOwnerId(publicId, ownerId)
                .ifPresent(publicSkillRepository::delete);
        skillRepository.delete(skill);
        deleteFilesAfterCommit(skill.getStoragePath());
    }

    private void deleteFilesAfterCommit(String storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.deleteSkillFiles(storagePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFilesQuietly(storagePath);
            }
        });
    }

    private boolean isShared(String ownerId, String publicId) {
        return publicSkillRepository.findBySourceSkillPublicIdAndPublisherOwnerId(publicId, ownerId).isPresent();
    }

    private void cleanUpAvatarAfterTransaction(String previousAvatarPath, String newAvatarPath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (previousAvatarPath != null && !previousAvatarPath.isBlank()) {
                deleteFileQuietly(previousAvatarPath);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    if (previousAvatarPath != null && !previousAvatarPath.isBlank()) {
                        deleteFileQuietly(previousAvatarPath);
                    }
                } else {
                    deleteFileQuietly(newAvatarPath);
                }
            }
        });
    }

    private void deleteFileAfterCommit(String storagePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.deleteFile(storagePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFileQuietly(storagePath);
            }
        });
    }

    private void deleteFileQuietly(String storagePath) {
        try {
            fileStorageService.deleteFile(storagePath);
        } catch (RuntimeException ignored) {
            // Database state is authoritative; an orphaned avatar can be cleaned up independently.
        }
    }

    private void deleteFilesQuietly(String storagePath) {
        try {
            fileStorageService.deleteSkillFiles(storagePath);
        } catch (RuntimeException ignored) {
            // Database state is authoritative; an orphaned file can be cleaned up independently.
        }
    }

    private String fileStoragePath(String ownerId, String publicId) {
        return fileStorageService.expectedPath(ownerId, publicId, "placeholder").toString();
    }

    static AvatarUpload readAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) return null;
        if (avatar.getSize() > MAX_AVATAR_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill 头像不能超过 2MB");
        }
        try {
            byte[] bytes = avatar.getBytes();
            AvatarFormat format = AvatarFormat.detect(bytes);
            if (format == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Skill 头像仅支持 PNG、JPEG、WebP 或 GIF");
            }
            return new AvatarUpload(bytes, format.contentType, format.extension);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取 Skill 头像", exception);
        }
    }

    enum AvatarFormat {
        PNG("image/png", "png"), JPEG("image/jpeg", "jpg"),
        WEBP("image/webp", "webp"), GIF("image/gif", "gif");

        private final String contentType;
        private final String extension;

        AvatarFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static AvatarFormat detect(byte[] bytes) {
            if (startsWith(bytes, new int[]{0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return PNG;
            if (startsWith(bytes, new int[]{0xff, 0xd8, 0xff})) return JPEG;
            if (startsWith(bytes, "GIF87a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    || startsWith(bytes, "GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return GIF;
            if (bytes.length >= 12 && startsWith(bytes, "RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return WEBP;
            return null;
        }

        private static boolean startsWith(byte[] bytes, int[] signature) {
            if (bytes.length < signature.length) return false;
            for (int index = 0; index < signature.length; index++) {
                if ((bytes[index] & 0xff) != signature[index]) return false;
            }
            return true;
        }

        private static boolean startsWith(byte[] bytes, byte[] signature) {
            if (bytes.length < signature.length) return false;
            for (int index = 0; index < signature.length; index++) {
                if (bytes[index] != signature[index]) return false;
            }
            return true;
        }
    }

    record AvatarUpload(byte[] bytes, String contentType, String extension) {
    }

    private static String safeText(String value, int maxLength) {
        return safeText(value, maxLength, "");
    }

    private static String safeText(String value, int maxLength, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.substring(0, Math.min(maxLength, normalized.length()));
    }

    private static String normalizeCategory(String value) {
        String category = safeText(value, 64, DEFAULT_CATEGORY);
        String normalized = switch (category) {
            case "编程开发" -> "编程技能";
            case "测试工具" -> "测试技能";
            case "排查工具" -> "排查技能";
            case "日志报告" -> "日志技能";
            default -> category;
        };
        return SUPPORTED_CATEGORIES.contains(normalized) ? normalized : DEFAULT_CATEGORY;
    }

    private static String requireCategory(String value) {
        String normalized = normalizeCategory(value);
        String requested = safeText(value, 64);
        if (requested.isEmpty() || (!SUPPORTED_CATEGORIES.contains(requested)
                && !Set.of("编程开发", "测试工具", "排查工具", "日志报告").contains(requested))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的 Skill 分类");
        }
        return normalized;
    }

    private static String requiredText(String value, int maxLength, String errorMessage) {
        String normalized = safeText(value, maxLength);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return normalized;
    }

    private static String normalizeUsageSteps(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.lines().filter(step -> !step.isBlank()).count() > MAX_USAGE_STEPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill 使用步骤不能超过 20 步");
        }
        if (value.lines().anyMatch(step -> step.trim().length() > 500)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每个 Skill 使用步骤不能超过 500 字");
        }
        return normalizeUsageSteps(value.lines().toList());
    }

    private static String normalizeUsageSteps(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        if (values.stream().filter(value -> value != null && !value.isBlank()).count() > MAX_USAGE_STEPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill 使用步骤不能超过 20 步");
        }
        if (values.stream().filter(java.util.Objects::nonNull).anyMatch(value -> value.trim().length() > 500)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每个 Skill 使用步骤不能超过 500 字");
        }
        List<String> normalized = values.stream()
                .map(value -> safeText(value, 500))
                .filter(value -> !value.isEmpty())
                .limit(MAX_USAGE_STEPS)
                .toList();
        return String.join("\n", normalized);
    }

    public record CategoryUpdateResult(SkillEntity skill, boolean publicPoolSynchronized) {
    }

    public record DetailUpdateResult(SkillEntity skill, boolean publicPoolSynchronized) {
    }

    public record AvatarUpdateResult(SkillEntity skill, boolean publicPoolSynchronized) {
    }
}
