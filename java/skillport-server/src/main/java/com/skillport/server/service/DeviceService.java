package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.security.TokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {
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
}
