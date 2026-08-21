package com.skillport.server.service;

import com.skillport.server.domain.PublicSkillEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.PublicSkillRepository;
import com.skillport.server.repository.SkillRepository;
import com.skillport.server.storage.FileStorageService;
import com.skillport.server.storage.StoredSkillFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PublicSkillService {
    private static final int PUBLIC_POOL_LIMIT = 100;

    private final PublicSkillRepository publicSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillService skillService;
    private final FileStorageService fileStorageService;

    public PublicSkillService(PublicSkillRepository publicSkillRepository, SkillRepository skillRepository,
                              SkillService skillService, FileStorageService fileStorageService) {
        this.publicSkillRepository = publicSkillRepository;
        this.skillRepository = skillRepository;
        this.skillService = skillService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<PublicSkillView> list(String ownerId) {
        List<PublicSkillEntity> publications = publicSkillRepository
                .findAllByOrderByPublishedAtDesc(PageRequest.of(0, PUBLIC_POOL_LIMIT));
        Set<String> importedIds = new HashSet<>(skillRepository.findImportedPublicSkillIds(ownerId));
        return publications.stream()
                .map(publication -> new PublicSkillView(publication,
                        publication.getPublisherOwnerId().equals(ownerId)
                                || importedIds.contains(publication.getPublicId())))
                .toList();
    }

    @Transactional
    public PublicSkillEntity share(String ownerId, String publisherDisplayName, String skillPublicId) {
        SkillEntity source = skillService.ownedSkill(ownerId, skillPublicId);
        return publicSkillRepository.findBySourceSkillPublicId(source.getPublicId())
                .orElseGet(() -> publicSkillRepository.save(new PublicSkillEntity(
                        UUID.randomUUID().toString(), source, safeDisplayName(publisherDisplayName), Instant.now())));
    }

    @Transactional
    public PullResult pull(String ownerId, String publicSkillId) {
        PublicSkillEntity publication = publicSkillRepository.findByPublicId(publicSkillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公有池 Skill 不存在"));
        SkillEntity source = skillRepository.findByPublicId(publication.getSourceSkillPublicId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "来源 Skill 已不可用"));

        if (publication.getPublisherOwnerId().equals(ownerId)) {
            return new PullResult(source, false);
        }
        SkillEntity existing = skillRepository.findByOwnerIdAndSourcePublicSkillId(ownerId, publicSkillId)
                .orElse(null);
        if (existing != null) {
            return new PullResult(existing, false);
        }

        String importedPublicId = UUID.randomUUID().toString();
        StoredSkillFile stored = fileStorageService.copy(
                ownerId, importedPublicId, source.getFileName(), source.getStoragePath());
        Instant now = Instant.now();
        SkillEntity imported = new SkillEntity(importedPublicId, ownerId, publication.getName(),
                publication.getDescription(), publication.getCategory(), publication.getFileName(),
                stored.path().toString(), publication.getContentType(), stored.sizeBytes(), stored.sha256(),
                publicSkillId, now);
        SkillEntity saved = skillRepository.save(imported);
        publication.recordPull(now);
        return new PullResult(saved, true);
    }

    @Transactional(readOnly = true)
    public Set<String> sharedSourceSkillIds(String ownerId) {
        return publicSkillRepository.findAllByPublisherOwnerId(ownerId).stream()
                .map(PublicSkillEntity::getSourceSkillPublicId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String safeDisplayName(String value) {
        String normalized = value == null || value.isBlank() ? "SkillPort 用户" : value.trim();
        return normalized.substring(0, Math.min(120, normalized.length()));
    }

    public record PublicSkillView(PublicSkillEntity publication, boolean pulled) {
    }

    public record PullResult(SkillEntity skill, boolean created) {
    }
}
