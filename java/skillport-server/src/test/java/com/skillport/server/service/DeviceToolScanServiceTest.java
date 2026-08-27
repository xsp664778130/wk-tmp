package com.skillport.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceToolScanServiceTest {
    @Test
    void verifiesDeviceOwnershipBeforeDispatchingTheNettyScan() {
        DeviceService deviceService = mock(DeviceService.class);
        BridgeSessionRegistry sessions = mock(BridgeSessionRegistry.class);
        DeviceEntity device = new DeviceEntity("device-1", "owner-1", "MacBook", "macos", "arm64",
                "hash", Instant.parse("2026-08-21T00:00:00Z"));
        when(deviceService.ownedDevice("owner-1", "device-1")).thenReturn(device);
        when(sessions.send(eq("device-1"), anyString())).thenReturn(true);
        DeviceToolScanService service = new DeviceToolScanService(
                deviceService, sessions, new ObjectMapper().registerModule(new JavaTimeModule()));

        DeviceToolScanService.ScanRequest request = service.request("owner-1", "device-1");

        assertEquals("device-1", request.deviceId());
        verify(deviceService).ownedDevice("owner-1", "device-1");
        verify(sessions).send(eq("device-1"), anyString());
    }

    @Test
    void reportsAnOfflineBridgeInsteadOfPretendingTheScanSucceeded() {
        DeviceService deviceService = mock(DeviceService.class);
        BridgeSessionRegistry sessions = mock(BridgeSessionRegistry.class);
        when(deviceService.ownedDevice("owner-1", "device-1")).thenReturn(mock(DeviceEntity.class));
        when(sessions.send(eq("device-1"), anyString())).thenReturn(false);
        DeviceToolScanService service = new DeviceToolScanService(
                deviceService, sessions, new ObjectMapper().registerModule(new JavaTimeModule()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.request("owner-1", "device-1"));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void storesOnlySupportedToolsInAStableOrder() {
        DeviceRepository repository = mock(DeviceRepository.class);
        DeviceEntity device = new DeviceEntity("device-1", "owner-1", "MacBook", "macos", "arm64",
                "hash", Instant.parse("2026-08-21T00:00:00Z"));
        when(repository.findByPublicId("device-1")).thenReturn(Optional.of(device));
        DeviceService service = new DeviceService(repository, mock(TokenService.class));
        Instant detectedAt = Instant.parse("2026-08-21T01:02:03Z");

        service.updateInstalledTools(
                "device-1",
                List.of("qoder", "unknown", "claude", "cursor", "codex", "opencode", "qoder"),
                detectedAt);

        assertEquals(List.of("claude", "codex", "cursor", "opencode", "qoder"), device.getInstalledTools());
        assertEquals(detectedAt, device.getToolsDetectedAt());
        assertTrue(device.getInstalledTools().stream().noneMatch("unknown"::equals));
    }
}
