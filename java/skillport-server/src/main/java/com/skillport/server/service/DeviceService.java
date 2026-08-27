package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DeviceService {
    private static final Set<String> SUPPORTED_TOOLS = Set.of(
            "codex", "qoder", "opencode", "claude", "cursor");
    private final DeviceRepository deviceRepository;
    private final TokenService tokenService;

    public DeviceService(DeviceRepository deviceRepository, TokenService tokenService) {
        this.deviceRepository = deviceRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public List<DeviceEntity> list(String ownerId) {
        return deviceRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Optional<DeviceEntity> authenticate(String deviceId, String rawToken) {
        return deviceRepository.findByPublicId(deviceId)
                .filter(device -> rawToken != null && tokenService.matches(rawToken, device.getTokenHash()));
    }

    @Transactional(readOnly = true)
    public DeviceEntity ownedDevice(String ownerId, String deviceId) {
        return deviceRepository.findByPublicIdAndOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
    }

    @Transactional
    public void markOnline(String deviceId) {
        deviceRepository.findByPublicId(deviceId).ifPresent(device -> device.markOnline(Instant.now()));
    }

    @Transactional
    public void markOffline(String deviceId) {
        deviceRepository.findByPublicId(deviceId).ifPresent(device -> device.markOffline(Instant.now()));
    }

    @Transactional
    public void heartbeat(String deviceId) {
        deviceRepository.findByPublicId(deviceId).ifPresent(device -> device.markOnline(Instant.now()));
    }

    @Transactional
    public void updateInstalledTools(String deviceId, List<String> tools, Instant detectedAt) {
        List<String> normalized = tools == null ? List.of() : tools.stream()
                .filter(SUPPORTED_TOOLS::contains)
                .distinct()
                .sorted()
                .toList();
        Instant scanTime = detectedAt == null ? Instant.now() : detectedAt;
        deviceRepository.findByPublicId(deviceId)
                .ifPresent(device -> device.updateInstalledTools(normalized, scanTime));
    }
}
