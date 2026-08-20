package com.skillport.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.protocol.InstallCommand;
import com.skillport.protocol.InstallProgress;
import com.skillport.protocol.MessageType;
import com.skillport.protocol.ProtocolCodec;
import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.domain.InstallTaskEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.repository.InstallTaskRepository;
import com.skillport.server.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InstallTaskService {
    private static final Set<String> ALLOWED_TARGETS = Set.of("codex", "qoder", "openai");
    private final InstallTaskRepository taskRepository;
    private final SkillRepository skillRepository;
    private final DeviceRepository deviceRepository;
    private final DownloadTicketService ticketService;
    private final BridgeSessionRegistry sessionRegistry;
    private final ProtocolCodec protocolCodec;

    public InstallTaskService(InstallTaskRepository taskRepository, SkillRepository skillRepository,
                              DeviceRepository deviceRepository, DownloadTicketService ticketService,
                              BridgeSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.skillRepository = skillRepository;
        this.deviceRepository = deviceRepository;
        this.ticketService = ticketService;
        this.sessionRegistry = sessionRegistry;
        this.protocolCodec = new ProtocolCodec(objectMapper);
    }

    @Transactional
    public InstallTaskEntity create(String ownerId, String skillId, String deviceId, List<String> requestedTargets) {
        List<String> targets = normalizeTargets(requestedTargets);
        SkillEntity skill = skillRepository.findByPublicIdAndOwnerId(skillId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill 不存在"));
        DeviceEntity device = resolveDevice(ownerId, deviceId);
        if (!sessionRegistry.isOnline(device.getPublicId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "目标设备当前不在线");
        }

        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        DownloadTicketService.IssuedTicket ticket = ticketService.issue(ownerId, skillId, device.getPublicId());
        InstallTaskEntity task = taskRepository.save(new InstallTaskEntity(taskId, ownerId, skillId,
                device.getPublicId(), String.join(",", targets), now));
        InstallCommand command = new InstallCommand(taskId, skillId, skill.getName(), skill.getFileName(),
                ticket.downloadUrl(), skill.getSha256(), skill.getSizeBytes(), targets, ticket.expiresAt());
        String message = protocolCodec.encode(MessageType.INSTALL_SKILL, taskId, command);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (sessionRegistry.send(device.getPublicId(), message)) markSent(taskId);
            }
        });
        return task;
    }

    @Transactional
    public void markSent(String taskId) {
        taskRepository.findByPublicId(taskId).ifPresent(task -> {
            task.markSent(Instant.now());
            taskRepository.save(task);
        });
    }

    @Transactional
    public void updateProgress(String deviceId, InstallProgress progress, boolean failed) {
        taskRepository.findByPublicIdAndDevicePublicId(progress.taskId(), deviceId).ifPresent(task -> {
            if (failed) task.fail(progress.message(), Instant.now());
            else task.updateProgress(progress.progress(), progress.stage(), Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public List<InstallTaskEntity> recent(String ownerId) {
        return taskRepository.findTop50ByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    private DeviceEntity resolveDevice(String ownerId, String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceRepository.findByPublicIdAndOwnerId(deviceId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
        }
        return deviceRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(device -> sessionRegistry.isOnline(device.getPublicId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "没有在线设备"));
    }

    private static List<String> normalizeTargets(List<String> requested) {
        if (requested == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择目标工具");
        List<String> targets = new LinkedHashSet<>(requested).stream().filter(ALLOWED_TARGETS::contains).toList();
        if (targets.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的目标工具");
        return targets;
    }
}
