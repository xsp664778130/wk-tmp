package com.skillport.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UninstallCommand(
        String taskId,
        String skillId,
        String skillName,
        String skillSlug,
        List<String> targets
) {
    public UninstallCommand {
        targets = List.copyOf(targets);
    }

    public UninstallCommand(String taskId, String skillId, String skillName, List<String> targets) {
        this(taskId, skillId, skillName, null, targets);
    }
}
