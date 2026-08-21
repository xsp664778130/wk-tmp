package com.skillport.server.service;

import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.SkillRepository;
import com.skillport.server.storage.FileStorageService;
import com.skillport.server.storage.StoredSkillFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SkillService {
    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    private static final String DEFAULT_CATEGORY = "编程开发";
    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "编程开发", "测试工具", "排查工具", "日志报告");
    private final SkillRepository skillRepository;
    private final FileStorageService fileStorageService;

    public SkillService(SkillRepository skillRepository, FileStorageService fileStorageService) {
        this.skillRepository = skillRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<SkillEntity> list(String ownerId) {
        return skillRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional
    public SkillEntity upload(String ownerId, String name, String description, String category, MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill 文件为空或超过 25MB");
        }
        String publicId = UUID.randomUUID().toString();
        try {
            StoredSkillFile stored = fileStorageService.store(ownerId, publicId, file.getOriginalFilename(), file.getInputStream());
            Instant now = Instant.now();
            String originalFilename = file.getOriginalFilename() == null ? "skill.zip" : file.getOriginalFilename();
            SkillEntity skill = new SkillEntity(
                    publicId, ownerId, requiredText(name, "Skill 名称不能为空"), safeText(description, 2000),
                    normalizeCategory(category), originalFilename, stored.path().toString(),
                    safeText(file.getContentType(), 120, "application/octet-stream"), stored.sizeBytes(), stored.sha256(), now);
            return skillRepository.save(skill);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取上传文件", exception);
        }
    }

    @Transactional
    public SkillEntity updateNote(String ownerId, String publicId, String note) {
        SkillEntity skill = ownedSkill(ownerId, publicId);
        skill.updateNote(safeText(note, 2000), Instant.now());
        return skill;
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

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return safeText(value, 160);
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
        return SUPPORTED_CATEGORIES.contains(category) ? category : DEFAULT_CATEGORY;
    }
}
