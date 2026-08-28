package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.security.TokenService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceServiceTest {
    @Test
    void collapsesRepeatedLegacyPairingsFromTheSameComputer() {
        DeviceRepository repository = mock(DeviceRepository.class);
        DeviceService service = new DeviceService(repository, mock(TokenService.class));
        DeviceEntity newest = legacy("device-4", "2026-08-28T04:00:00Z");
        List<DeviceEntity> records = List.of(
                newest,
                legacy("device-3", "2026-08-28T03:00:00Z"),
                legacy("device-2", "2026-08-28T02:00:00Z"),
                legacy("device-1", "2026-08-28T01:00:00Z"));
        when(repository.findAllByOwnerIdOrderByCreatedAtDesc("owner-1")).thenReturn(records);

        List<DeviceEntity> logicalDevices = service.list("owner-1");

        assertEquals(1, logicalDevices.size());
        assertEquals("device-4", logicalDevices.getFirst().getPublicId());
    }

    @Test
    void keepsDifferentStableInstancesEvenWhenTheirDisplayNamesMatch() {
        DeviceEntity first = stable("device-1", "instance-1111");
        DeviceEntity second = stable("device-2", "instance-2222");

        List<DeviceEntity> logicalDevices = DeviceService.logicalDevices(List.of(first, second));

        assertEquals(List.of("device-1", "device-2"),
                logicalDevices.stream().map(DeviceEntity::getPublicId).toList());
    }

    private static DeviceEntity legacy(String publicId, String createdAt) {
        return new DeviceEntity(publicId, "owner-1", "徐世鹏的Mac mini", "macos", "aarch64",
                "hash", Instant.parse(createdAt));
    }

    private static DeviceEntity stable(String publicId, String instanceId) {
        return new DeviceEntity(publicId, "owner-1", instanceId, "徐世鹏的Mac mini", "macos", "aarch64",
                "hash", Instant.parse("2026-08-28T04:00:00Z"));
    }
}
