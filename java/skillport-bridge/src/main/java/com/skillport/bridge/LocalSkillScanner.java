package com.skillport.bridge;

import com.skillport.protocol.LocalSkillInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LocalSkillScanner {
    private static final int MAX_SKILLS_PER_TOOL = 100;
    private static final int MAX_MANIFEST_BYTES = 64 * 1024;
    private static final Set<String> SUPPORTED_TOOLS = Set.of(
            "codex", "qoder", "opencode", "claude", "cursor");
    static final String ORIGIN_MARKER = ".skillport-origin";

    private final Path home;

    public LocalSkillScanner() {
        this(Path.of(System.getProperty("user.home")));
    }

    LocalSkillScanner(Path home) {
        this.home = home.toAbsolutePath().normalize();
    }

    public List<LocalSkillInfo> scan(List<String> detectedTools) {
        if (detectedTools == null || detectedTools.isEmpty()) return List.of();
        List<LocalSkillInfo> result = new ArrayList<>();
        detectedTools.stream()
                .filter(SUPPORTED_TOOLS::contains)
                .distinct()
                .sorted()
                .forEach(tool -> result.addAll(scanTool(tool)));
        return List.copyOf(result);
    }

    private List<LocalSkillInfo> scanTool(String tool) {
        Path root = ToolTargetPaths.root(home, tool);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return List.of();
        try (var children = Files.list(root)) {
            return children
                    .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .limit(MAX_SKILLS_PER_TOOL)
                    .map(path -> inspect(tool, root, path))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private LocalSkillInfo inspect(String tool, Path root, Path skillDirectory) {
        Path manifest = findManifest(skillDirectory);
        if (manifest == null) return null;
        String slug = skillDirectory.getFileName().toString();
        ManifestMetadata metadata = readMetadata(manifest, slug);
        String relativePath = "~/" + home.relativize(skillDirectory).toString().replace('\\', '/');
        return new LocalSkillInfo(tool, slug, metadata.name(), metadata.description(), relativePath,
                readOriginSkillId(skillDirectory.resolve(ORIGIN_MARKER)));
    }

    private static Path findManifest(Path skillDirectory) {
        try (var paths = Files.find(skillDirectory, 3,
                (path, attributes) -> attributes.isRegularFile()
                        && path.getFileName().toString().equalsIgnoreCase("SKILL.md"))) {
            return paths.findFirst().orElse(null);
        } catch (IOException exception) {
            return null;
        }
    }

    private static ManifestMetadata readMetadata(Path manifest, String fallbackName) {
        try {
            byte[] bytes;
            try (var input = Files.newInputStream(manifest)) {
                bytes = input.readNBytes(MAX_MANIFEST_BYTES);
            }
            String content = new String(bytes, StandardCharsets.UTF_8);
            String name = frontmatterValue(content, "name");
            String description = frontmatterValue(content, "description");
            return new ManifestMetadata(name.isBlank() ? fallbackName : name,
                    description.isBlank() ? "本机 Skill" : description);
        } catch (IOException exception) {
            return new ManifestMetadata(fallbackName, "本机 Skill");
        }
    }

    private static String frontmatterValue(String content, String key) {
        if (!content.startsWith("---")) return "";
        int end = content.indexOf("\n---", 3);
        if (end < 0) return "";
        String prefix = key + ":";
        return content.substring(3, end).lines()
                .map(String::trim)
                .filter(line -> line.toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim().replaceAll("^[\\\"']|[\\\"']$", ""))
                .findFirst()
                .orElse("");
    }

    private static String readOriginSkillId(Path marker) {
        try {
            if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) return null;
            String value = Files.readString(marker, StandardCharsets.UTF_8).trim();
            return value.matches("[a-zA-Z0-9-]{1,64}") ? value : null;
        } catch (IOException exception) {
            return null;
        }
    }

    private record ManifestMetadata(String name, String description) {
    }
}
