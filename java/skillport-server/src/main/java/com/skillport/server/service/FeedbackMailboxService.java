package com.skillport.server.service;

import com.skillport.server.domain.FeedbackMessageEntity;
import com.skillport.server.repository.FeedbackMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedbackMailboxService {
    private static final Set<String> SUPPORTED_KINDS = Set.of("功能建议", "问题反馈", "体验优化", "其他");
    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 20;
    private final FeedbackMessageRepository repository;

    public FeedbackMailboxService(FeedbackMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FeedbackMessageEntity submit(String ownerId, String submitterDisplayName, String kind, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少填写 5 个字的意见");
        }
        if (normalizedContent.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "意见内容不能超过 2000 个字");
        }
        String normalizedKind = kind == null ? "" : kind.trim();
        if (!SUPPORTED_KINDS.contains(normalizedKind)) {
            normalizedKind = "其他";
        }
        String normalizedDisplayName = submitterDisplayName == null ? "" : submitterDisplayName.trim();
        if (normalizedDisplayName.isEmpty()) normalizedDisplayName = "SkillPort 用户";
        if (normalizedDisplayName.length() > 120) normalizedDisplayName = normalizedDisplayName.substring(0, 120);
        return repository.save(new FeedbackMessageEntity(
                UUID.randomUUID().toString(), ownerId, normalizedDisplayName,
                normalizedKind, normalizedContent, Instant.now()));
    }

    @Transactional(readOnly = true)
    public Page<FeedbackMessageEntity> list(int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1) - 1;
        int size = requestedSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(requestedSize, MAX_PAGE_SIZE);
        return repository.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }
}
