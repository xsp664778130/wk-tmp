package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.domain.PairingCodeEntity;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.repository.PairingCodeRepository;
import com.skillport.server.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PairingService {
    private static final Duration PAIRING_TTL = Duration.ofMinutes(10);
    private final PairingCodeRepository pairingCodeRepository;
    private final DeviceRepository deviceRepository;
    private final TokenService tokenService;

    public PairingService(PairingCodeRepository pairingCodeRepository, DeviceRepository deviceRepository,
                          TokenService tokenService) {
        this.pairingCodeRepository = pairingCodeRepository;
        this.deviceRepository = deviceRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public PairingCode createCode(String ownerId) {
        String raw = tokenService.randomToken(6).replace('-', 'A').replace('_', 'B').toUpperCase(Locale.ROOT);
        String display = raw.substring(0, 4) + "-" + raw.substring(4);
        Instant now = Instant.now();
        pairingCodeRepository.save(new PairingCodeEntity(tokenService.sha256(normalizeCode(display)), ownerId,
                now.plus(PAIRING_TTL), now));
        return new PairingCode(display, now.plus(PAIRING_TTL));
    }

    @Transactional
    public PairedDevice pair(String code, String name, String os, String arch) {
        Instant now = Instant.now();
        PairingCodeEntity pairing = pairingCodeRepository.findById(tokenService.sha256(normalizeCode(code)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "配对码无效"));
        if (!pairing.isUsable(now)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "配对码已过期或已使用");
        pairing.consume(now);

        String rawToken = tokenService.randomToken(32);
        DeviceEntity device = new DeviceEntity(UUID.randomUUID().toString(), pairing.getOwnerId(),
                text(name, 120, "SkillPort Bridge"), text(os, 32, "unknown"), text(arch, 32, "unknown"),
                tokenService.sha256(rawToken), now);
        deviceRepository.save(device);
        return new PairedDevice(device.getPublicId(), rawToken);
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.replace("-", "").trim().toUpperCase(Locale.ROOT);
    }

    private static String text(String value, int maxLength, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.substring(0, Math.min(maxLength, normalized.length()));
    }

    public record PairingCode(String code, Instant expiresAt) {
    }
    public record PairedDevice(String deviceId, String deviceToken) {
    }
}
