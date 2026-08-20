package com.skillport.server.web;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.netty.BridgeSessionRegistry;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.DeviceService;
import com.skillport.server.service.PairingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final PairingService pairingService;
    private final BridgeSessionRegistry sessionRegistry;
    private final SkillPortProperties properties;

    public DeviceController(DeviceService deviceService, PairingService pairingService,
                            BridgeSessionRegistry sessionRegistry, SkillPortProperties properties) {
        this.deviceService = deviceService;
        this.pairingService = pairingService;
        this.sessionRegistry = sessionRegistry;
        this.properties = properties;
    }

    @GetMapping
    public DeviceListResponse list(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        List<DeviceResponse> devices = deviceService.list(user.userId()).stream().map(device -> DeviceResponse.from(
                device, sessionRegistry.isOnline(device.getPublicId()))).toList();
        return new DeviceListResponse(devices);
    }

    @PostMapping("/pairing-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public PairingCodeResponse pairingCode(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        PairingService.PairingCode pairingCode = pairingService.createCode(user.userId());
        return new PairingCodeResponse(pairingCode.code(), pairingCode.expiresAt(),
                properties.publicApiBaseUrl(), properties.publicNettyBaseUrl());
    }

    public record DeviceListResponse(List<DeviceResponse> devices) {
    }
    public record PairingCodeResponse(String code, Instant expiresAt, String apiBaseUrl, String nettyUrl) {
    }
    public record DeviceResponse(String id, String name, String os, String arch, String status, Instant lastSeenAt) {
        static DeviceResponse from(DeviceEntity device, boolean online) {
            return new DeviceResponse(device.getPublicId(), device.getName(), device.getOs(), device.getArch(),
                    online ? "ONLINE" : "OFFLINE", device.getLastSeenAt());
        }
    }
}
