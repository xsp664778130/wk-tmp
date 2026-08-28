package com.skillport.server.service;

import com.skillport.protocol.LocalSkillInfo;
import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.domain.DeviceLocalSkillEntity;
import com.skillport.server.domain.InstallTaskEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.DeviceLocalSkillRepository;
import com.skillport.server.repository.InstallTaskRepository;
import com.skillport.server.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LocalSkillWorkspaceService {
    private static final int MAX_LOCAL_SKILLS = 500;
    private static final Set<String> SUPPORTED_TOOLS = Set.of(
            "codex", "qoder", "opencode", "claude", "cursor");

    private final DeviceLocalSkillRepository localSkillRepository;
    private final SkillRepository skillRepository;
    private final InstallTaskRepository installTaskRepository;
    private final DeviceService deviceService;

    public LocalSkillWorkspaceService(DeviceLocalSkillRepository localSkillRepository,
                                      SkillRepository skillRepository,
                                      InstallTaskRepository installTaskRepository,
                                      DeviceService deviceService) {
        this.localSkillRepository = localSkillRepository;
        this.skillRepository = skillRepository;
        this.installTaskRepository = installTaskRepository;
        this.deviceService = deviceService;
    }

    @Transactional
    public void replaceInventory(String deviceId, List<String> tools, List<LocalSkillInfo> values, Instant detectedAt) {
        DeviceEntity device = deviceService.device(deviceId);
        Instant scanTime = detectedAt == null ? Instant.now() : detectedAt;
        List<String> normalizedTools = tools == null ? List.of() : tools.stream()
                .map(LocalSkillWorkspaceService::normalizeTool)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .toList();
        device.updateInstalledTools(normalizedTools, scanTime);
        Map<String, DeviceLocalSkillEntity> unique = new LinkedHashMap<>();
        if (values != null) {
            values.stream().limit(MAX_LOCAL_SKILLS)
                    .map(value -> normalize(device, value, scanTime))
                    .filter(java.util.Objects::nonNull)
                    .forEach(value -> unique.put(value.getTool() + "\u0000" + value.getSlug(), value));
        }
        localSkillRepository.deleteInventory(deviceId);
        if (!unique.isEmpty()) localSkillRepository.saveAll(unique.values());
    }

    @Transactional(readOnly = true)
    public WorkspaceView workspace(String ownerId, String deviceId) {
        DeviceEntity device = deviceService.ownedDevice(ownerId, deviceId);
        List<DeviceLocalSkillEntity> inventory = localSkillRepository
                .findAllByOwnerIdAndDevicePublicIdOrderByToolAscNameAsc(ownerId, deviceId);
        List<SkillEntity> ownedSkills = skillRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        Map<String, SkillEntity> ownedById = new HashMap<>();
        Map<String, List<SkillEntity>> ownedBySlug = new HashMap<>();
        for (SkillEntity skill : ownedSkills) {
            ownedById.put(skill.getPublicId(), skill);
            ownedBySlug.computeIfAbsent(slug(skill.getName()), ignored -> new ArrayList<>()).add(skill);
        }
        Set<String> installedTaskKeys = completedInstallKeys(ownerId, deviceId, ownedById);
        List<LocalSkillView> skills = inventory.stream().map(localSkill -> {
            SkillEntity source = ownedById.get(localSkill.getOriginSkillId());
            if (source == null) {
                source = ownedBySlug.getOrDefault(localSkill.getSlug(), List.of()).stream()
                        .filter(skill -> installedTaskKeys.contains(
                                localSkill.getTool() + "\u0000" + skill.getPublicId()))
                        .findFirst()
                        .orElse(null);
            }
            return LocalSkillView.from(localSkill, source);
        }).toList();
        return new WorkspaceView(device.getPublicId(), device.getName(), device.getToolsDetectedAt(), skills);
    }

    @Transactional(readOnly = true)
    public DeviceLocalSkillEntity ownedLocalSkill(String ownerId, String deviceId, String tool, String slug) {
        deviceService.ownedDevice(ownerId, deviceId);
        return localSkillRepository.findByOwnerIdAndDevicePublicIdAndToolAndSlug(
                        ownerId, deviceId, normalizeTool(tool), normalizeSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "本机 Skill 不存在，请重新识别"));
    }

    private Set<String> completedInstallKeys(String ownerId, String deviceId, Map<String, SkillEntity> ownedById) {
        List<InstallTaskEntity> tasks = installTaskRepository
                .findTop200ByOwnerIdAndDevicePublicIdAndStatusOrderByUpdatedAtDesc(ownerId, deviceId, "COMPLETED");
        Map<String, String> latestOperations = new HashMap<>();
        for (InstallTaskEntity task : tasks) {
            if (!ownedById.containsKey(task.getSkillPublicId())) continue;
            for (String target : task.getTargets().split(",")) {
                latestOperations.putIfAbsent(target + "\u0000" + task.getSkillPublicId(), task.getOperation());
            }
        }
        Set<String> installed = new HashSet<>();
        latestOperations.forEach((key, operation) -> {
            if ("INSTALL".equals(operation)) installed.add(key);
        });
        return installed;
    }

    private static DeviceLocalSkillEntity normalize(DeviceEntity device, LocalSkillInfo value, Instant detectedAt) {
        if (value == null) return null;
        String tool = normalizeTool(value.tool());
        String slug = normalizeSlug(value.slug());
        if (tool.isEmpty() || slug.isEmpty()) return null;
        return new DeviceLocalSkillEntity(device.getOwnerId(), device.getPublicId(), tool, slug,
                text(value.name(), 200, slug), text(value.description(), 1000, "本机 Skill"),
                text(value.relativePath(), 512, ""), nullableText(value.originSkillId(), 64), detectedAt);
    }

    private static String normalizeTool(String value) {
        String normalized = text(value, 32, "").toLowerCase(java.util.Locale.ROOT);
        return SUPPORTED_TOOLS.contains(normalized) ? normalized : "";
    }

    private static String normalizeSlug(String value) {
        String normalized = text(value, 180, "");
        return !normalized.equals(".") && !normalized.equals("..")
                && normalized.matches("[a-zA-Z0-9._\\-\\p{IsHan}]+") ? normalized : "";
    }

    static String slug(String value) {
        String normalized = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9\\p{IsHan}]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "skillport-skill" : normalized;
    }

    private static String text(String value, int maxLength, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.substring(0, Math.min(maxLength, normalized.length()));
    }

    private static String nullableText(String value, int maxLength) {
        String normalized = text(value, maxLength, "");
        return normalized.isEmpty() ? null : normalized;
    }

    public record WorkspaceView(String deviceId, String deviceName, Instant detectedAt,
                                List<LocalSkillView> skills) {
    }

    public record LocalSkillView(String tool, String slug, String name, String description,
                                 String relativePath, boolean fromMySkills, String sourceSkillId) {
        static LocalSkillView from(DeviceLocalSkillEntity localSkill, SkillEntity source) {
            return new LocalSkillView(localSkill.getTool(), localSkill.getSlug(), localSkill.getName(),
                    localSkill.getDescription(), localSkill.getRelativePath(), source != null,
                    source == null ? null : source.getPublicId());
        }
    }
}
