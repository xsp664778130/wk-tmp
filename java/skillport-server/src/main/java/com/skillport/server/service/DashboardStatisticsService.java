package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.repository.InstallTaskRepository;
import com.skillport.server.repository.PublicSkillRepository;
import com.skillport.server.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardStatisticsService {
    private final SkillRepository skillRepository;
    private final PublicSkillRepository publicSkillRepository;
    private final InstallTaskRepository installTaskRepository;
    private final DeviceRepository deviceRepository;
    private final BridgeSessionRegistry sessionRegistry;

    public DashboardStatisticsService(SkillRepository skillRepository,
                                      PublicSkillRepository publicSkillRepository,
                                      InstallTaskRepository installTaskRepository,
                                      DeviceRepository deviceRepository,
                                      BridgeSessionRegistry sessionRegistry) {
        this.skillRepository = skillRepository;
        this.publicSkillRepository = publicSkillRepository;
        this.installTaskRepository = installTaskRepository;
        this.deviceRepository = deviceRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional(readOnly = true)
    public DashboardStatistics statistics(String ownerId) {
        List<DeviceEntity> devices = deviceRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        long onlineDevices = devices.stream()
                .filter(device -> sessionRegistry.isOnline(device.getPublicId()))
                .count();
        return new DashboardStatistics(
                skillRepository.countByOwnerId(ownerId),
                publicSkillRepository.countByPublisherOwnerId(ownerId),
                installTaskRepository.countByOwnerId(ownerId),
                devices.size(),
                onlineDevices);
    }

    public record DashboardStatistics(long mySkills, long sharedSkills, long totalInstalls,
                                      long connectedDevices, long onlineDevices) {
    }
}
