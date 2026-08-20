package com.skillport.protocol;

public record InstallProgress(
        String taskId,
        int progress,
        String stage,
        String message
) {
    public InstallProgress {
        progress = Math.max(0, Math.min(100, progress));
    }
}
