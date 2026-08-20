package com.skillport.server.service;

import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.domain.DownloadTicketEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.DownloadTicketRepository;
import com.skillport.server.repository.SkillRepository;
import com.skillport.server.security.TokenService;
import com.skillport.server.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Service
public class DownloadTicketService {
    private static final Duration TICKET_TTL = Duration.ofMinutes(10);
    private final DownloadTicketRepository ticketRepository;
    private final SkillRepository skillRepository;
    private final TokenService tokenService;
    private final FileStorageService storageService;
    private final SkillPortProperties properties;

    public DownloadTicketService(DownloadTicketRepository ticketRepository, SkillRepository skillRepository,
                                 TokenService tokenService, FileStorageService storageService,
                                 SkillPortProperties properties) {
        this.ticketRepository = ticketRepository;
        this.skillRepository = skillRepository;
        this.tokenService = tokenService;
        this.storageService = storageService;
        this.properties = properties;
    }

    @Transactional
    public IssuedTicket issue(String ownerId, String skillId, String deviceId) {
        String rawToken = tokenService.randomToken(32);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TICKET_TTL);
        ticketRepository.save(new DownloadTicketEntity(tokenService.sha256(rawToken), ownerId, skillId,
                deviceId, expiresAt, now));
        return new IssuedTicket(properties.publicNettyBaseUrl() + "/downloads/" + rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public DownloadGrant resolve(String rawToken) {
        DownloadTicketEntity ticket = ticketRepository.findById(tokenService.sha256(rawToken)).orElse(null);
        if (ticket == null || ticket.isExpired(Instant.now())) return null;
        SkillEntity skill = skillRepository.findByPublicId(ticket.getSkillPublicId()).orElse(null);
        if (skill == null || !skill.getOwnerId().equals(ticket.getOwnerId())) return null;
        Path path = storageService.resolve(skill.getStoragePath());
        return new DownloadGrant(path, skill.getFileName(), skill.getContentType(), skill.getSizeBytes(), skill.getSha256());
    }

    public record IssuedTicket(String downloadUrl, Instant expiresAt) {
    }
    public record DownloadGrant(Path path, String fileName, String contentType, long sizeBytes, String sha256) {
    }
}
