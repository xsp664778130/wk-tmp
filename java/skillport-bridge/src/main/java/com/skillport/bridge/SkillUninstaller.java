package com.skillport.bridge;

import com.skillport.protocol.UninstallCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class SkillUninstaller {
    private final UninstallProgressListener progressListener;
    private final Path home;

    public SkillUninstaller(UninstallProgressListener progressListener) {
        this(progressListener, Path.of(System.getProperty("user.home")));
    }

    SkillUninstaller(UninstallProgressListener progressListener, Path home) {
        this.progressListener = progressListener;
        this.home = home.toAbsolutePath().normalize();
    }

    public UninstallResult uninstall(UninstallCommand command) {
        String slug = ToolTargetPaths.slug(command.skillName());
        int removed = 0;
        int total = command.targets().size();
        progressListener.onProgress(10, "PREPARING", "正在检查本机 Skill 目录");
        for (int index = 0; index < total; index++) {
            String target = command.targets().get(index);
            Path installed = ToolTargetPaths.resolve(home, target, slug);
            if (Files.exists(installed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                deleteInstalledSkill(installed);
                removed++;
            }
            int progress = 20 + (int) Math.round(((index + 1) * 70.0) / Math.max(1, total));
            progressListener.onProgress(progress, "UNINSTALLING", "正在从 " + target + " 移除");
        }
        return new UninstallResult(removed, total);
    }

    private static void deleteInstalledSkill(Path source) {
        try {
            if (!Files.isDirectory(source, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                Files.delete(source);
                return;
            }
            List<Path> paths;
            try (var pathStream = Files.walk(source)) {
                paths = pathStream.sorted(Comparator.reverseOrder()).toList();
            }
            for (Path path : paths) {
                Files.delete(path);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法从本机移除 Skill：" + source, exception);
        }
    }

    public record UninstallResult(int removedTargets, int requestedTargets) {
    }

    @FunctionalInterface
    public interface UninstallProgressListener {
        void onProgress(int progress, String stage, String message);
    }
}
