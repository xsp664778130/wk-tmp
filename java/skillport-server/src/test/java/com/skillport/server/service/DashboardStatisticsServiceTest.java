package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import com.skillport.server.repository.InstallTaskRepository;
import com.skillport.server.repository.PublicSkillRepository;
import com.skillport.server.repository.SkillRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardStatisticsServiceTest {
    @Test
    void aggregatesOnlyTheCurrentOwnersPersistedDataAndLiveSessions() {
        SkillRepository skillRepository = mock(SkillRepository.class);
        PublicSkillRepository publicSkillRepository = mock(PublicSkillRepository.class);
        InstallTaskRepository installTaskRepository = mock(InstallTaskRepository.class);
        DeviceService deviceService = mock(DeviceService.class);
        BridgeSessionRegistry sessionRegistry = mock(BridgeSessionRegistry.class);
        DashboardStatisticsService service = new DashboardStatisticsService(
                skillRepository, publicSkillRepository, installTaskRepository, deviceService, sessionRegistry);

        String ownerId = "owner-1";
        DeviceEntity online = new DeviceEntity("device-online", ownerId, "MacBook", "macos", "arm64",
                "hash", Instant.parse("2026-08-21T00:00:00Z"));
        DeviceEntity offline = new DeviceEntity("device-offline", ownerId, "Windows", "windows", "amd64",
                "hash", Instant.parse("2026-08-21T00:00:00Z"));
        when(skillRepository.countByOwnerId(ownerId)).thenReturn(7L);
        when(publicSkillRepository.countByPublisherOwnerId(ownerId)).thenReturn(2L);
        when(installTaskRepository.countByOwnerId(ownerId)).thenReturn(5L);
        when(deviceService.list(ownerId)).thenReturn(List.of(online, offline));
        when(sessionRegistry.isOnline("device-online")).thenReturn(true);
        when(sessionRegistry.isOnline("device-offline")).thenReturn(false);

        DashboardStatisticsService.DashboardStatistics statistics = service.statistics(ownerId);

        assertEquals(7L, statistics.mySkills());
        assertEquals(2L, statistics.sharedSkills());
        assertEquals(5L, statistics.totalInstalls());
        assertEquals(2L, statistics.connectedDevices());
        assertEquals(1L, statistics.onlineDevices());
    }
}
