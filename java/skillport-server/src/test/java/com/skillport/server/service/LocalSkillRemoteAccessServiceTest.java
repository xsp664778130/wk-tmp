package com.skillport.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skillport.protocol.BridgeEnvelope;
import com.skillport.protocol.LocalSkillActionCommand;
import com.skillport.protocol.LocalSkillActionResult;
import com.skillport.protocol.MessageType;
import com.skillport.protocol.ProtocolCodec;
import com.skillport.server.domain.DeviceLocalSkillEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalSkillRemoteAccessServiceTest {
    @Test
    void readsManifestThroughTheOwnedOnlineDeviceWithoutPersistingContent() {
        LocalSkillWorkspaceService workspace = mock(LocalSkillWorkspaceService.class);
        BridgeSessionRegistry sessions = mock(BridgeSessionRegistry.class);
        DeviceLocalSkillEntity localSkill = new DeviceLocalSkillEntity(
                "owner-1", "device-1", "codex", "demo", "Demo", "Description",
                "~/.codex/skills/demo", null, Instant.parse("2026-08-28T00:00:00Z"));
        when(workspace.ownedLocalSkill("owner-1", "device-1", "codex", "demo")).thenReturn(localSkill);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ProtocolCodec codec = new ProtocolCodec(objectMapper);
        AtomicReference<LocalSkillRemoteAccessService> serviceReference = new AtomicReference<>();
        when(sessions.send(eq("device-1"), startsWith("{"))).thenAnswer(invocation -> {
            BridgeEnvelope envelope = codec.decode(invocation.getArgument(1));
            assertEquals(MessageType.READ_LOCAL_SKILL_MANIFEST, envelope.type());
            LocalSkillActionCommand command = codec.payload(envelope, LocalSkillActionCommand.class);
            assertEquals("codex", command.tool());
            assertEquals("demo", command.slug());
            serviceReference.get().complete("device-1", envelope.requestId(),
                    LocalSkillActionResult.manifest("codex", "demo", "# Demo\n"));
            return true;
        });
        LocalSkillRemoteAccessService service = new LocalSkillRemoteAccessService(workspace, sessions, objectMapper);
        serviceReference.set(service);

        LocalSkillActionResult result = service.readManifest("owner-1", "device-1", "codex", "demo");

        assertEquals("# Demo\n", result.content());
    }
}
