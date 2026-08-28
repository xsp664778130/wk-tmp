package com.skillport.bridge;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public final class ToolTargetPaths {
    private static final Map<String, String> TOOL_DIRECTORIES = Map.of(
            "codex", ".codex/skills",
            "qoder", ".qoder/skills",
            "opencode", ".config/opencode/skills",
            "claude", ".claude/skills",
            "cursor", ".cursor/skills"
    );

    private ToolTargetPaths() {
    }

    public static Path resolve(Path home, String target, String skillSlug) {
        Path resolved = root(home, target).resolve(skillSlug).normalize();
        if (!resolved.startsWith(home.normalize())) throw new IllegalArgumentException("无效的安装路径");
        return resolved;
    }

    public static Path root(Path home, String target) {
        String directory = TOOL_DIRECTORIES.get(target);
        if (directory == null) throw new IllegalArgumentException("不支持的 AI 工具: " + target);
        Path resolved = home.resolve(directory).normalize();
        if (!resolved.startsWith(home.normalize())) throw new IllegalArgumentException("无效的安装路径");
        return resolved;
    }

    public static String slug(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\p{IsHan}]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "skillport-skill" : normalized;
    }

    public static String literalSlug(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.equals(".") || normalized.equals("..")
                || !normalized.matches("[a-zA-Z0-9._\\-\\p{IsHan}]+")) {
            throw new IllegalArgumentException("无效的本机 Skill 目录名称");
        }
        return normalized;
    }
}
