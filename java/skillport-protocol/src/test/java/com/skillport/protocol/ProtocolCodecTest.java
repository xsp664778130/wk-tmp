package com.skillport.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolCodecTest {
    @Test
    void roundTripsInstallCommand() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ProtocolCodec codec = new ProtocolCodec(objectMapper);
        InstallCommand command = new InstallCommand(
                "task-1", "skill-1", "API Architect", "skill.zip",
                "https://example.test/download", "abc", 128L, List.of("codex"), Instant.now().plusSeconds(60));

        BridgeEnvelope envelope = codec.decode(codec.encode(MessageType.INSTALL_SKILL, "task-1", command));
        InstallCommand decoded = codec.payload(envelope, InstallCommand.class);

        assertEquals(MessageType.INSTALL_SKILL, envelope.type());
        assertEquals(command.taskId(), decoded.taskId());
        assertEquals(command.targets(), decoded.targets());
    }
}
