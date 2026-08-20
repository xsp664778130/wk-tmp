package com.skillport.protocol;

import java.time.Instant;
import java.util.List;

public record InstallCommand(
        String taskId,
        String skillId,
        String skillName,
        String fileName,
        String downloadUrl,
        String sha256,
        long sizeBytes,
        List<String> targets,
        Instant expiresAt
) {
    public InstallCommand {
        targets = List.copyOf(targets);
    }
}
