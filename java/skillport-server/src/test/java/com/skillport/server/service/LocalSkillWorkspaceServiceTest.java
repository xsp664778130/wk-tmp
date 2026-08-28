package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.domain.DeviceLocalSkillEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.DeviceLocalSkillRepository;
import com.skillport.server.repository.InstallTaskRepository;
import com.skillport.server.repository.SkillRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalSkillWorkspaceServiceTest {
    @Test
    void returnsOnlyTheSelectedOwnersDeviceInventoryAndMarksOwnedOrigins() {
        DeviceLocalSkillRepository localSkills = mock(DeviceLocalSkillRepository.class);
        SkillRepository skills = mock(SkillRepository.class);
        InstallTaskRepository tasks = mock(InstallTaskRepository.class);
        DeviceService devices = mock(DeviceService.class);
        LocalSkillWorkspaceService service = new LocalSkillWorkspaceService(localSkills, skills, tasks, devices);
        Instant detectedAt = Instant.parse("2026-08-28T08:00:00Z");
        DeviceEntity device = new DeviceEntity("device-1", "owner-1", "Mac mini", "macos", "arm64",
                "hash", detectedAt);
        device.updateInstalledTools(List.of("codex", "cursor"), detectedAt);
        SkillEntity owned = new SkillEntity("skill-1", "owner-1", "My Audit", "description",
                "排查技能", "skill.zip", "/tmp/skill.zip", "application/zip", 1, "hash", detectedAt);
        DeviceLocalSkillEntity fromMySkills = new DeviceLocalSkillEntity(
                "owner-1", "device-1", "codex", "my-audit", "My Audit", "description",
                "~/.codex/skills/my-audit", "skill-1", detectedAt);
        DeviceLocalSkillEntity external = new DeviceLocalSkillEntity(
                "owner-1", "device-1", "cursor", "external", "External", "local only",
                "~/.cursor/skills/external", null, detectedAt);
        when(devices.ownedDevice("owner-1", "device-1")).thenReturn(device);
        when(localSkills.findAllByOwnerIdAndDevicePublicIdOrderByToolAscNameAsc("owner-1", "device-1"))
                .thenReturn(List.of(fromMySkills, external));
        when(skills.findAllByOwnerIdOrderByCreatedAtDesc("owner-1")).thenReturn(List.of(owned));
        when(tasks.findTop200ByOwnerIdAndDevicePublicIdAndStatusOrderByUpdatedAtDesc(
                "owner-1", "device-1", "COMPLETED")).thenReturn(List.of());

        LocalSkillWorkspaceService.WorkspaceView workspace = service.workspace("owner-1", "device-1");

        assertEquals("device-1", workspace.deviceId());
        assertEquals(2, workspace.skills().size());
        assertTrue(workspace.skills().getFirst().fromMySkills());
        assertEquals("skill-1", workspace.skills().getFirst().sourceSkillId());
        assertFalse(workspace.skills().get(1).fromMySkills());
        verify(localSkills).findAllByOwnerIdAndDevicePublicIdOrderByToolAscNameAsc("owner-1", "device-1");
    }
}
