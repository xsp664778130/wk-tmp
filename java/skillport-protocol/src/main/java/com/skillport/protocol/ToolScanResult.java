package com.skillport.protocol;

import java.time.Instant;
import java.util.List;

public record ToolScanResult(List<String> tools, List<LocalSkillInfo> skills, Instant detectedAt) {
    public ToolScanResult {
        tools = tools == null ? List.of() : List.copyOf(tools);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public ToolScanResult(List<String> tools, Instant detectedAt) {
        this(tools, List.of(), detectedAt);
    }
}
