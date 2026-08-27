package com.skillport.protocol;

import java.util.List;

public record UninstallCommand(
        String taskId,
        String skillId,
        String skillName,
        List<String> targets
) {
    public UninstallCommand {
        targets = List.copyOf(targets);
    }
}
