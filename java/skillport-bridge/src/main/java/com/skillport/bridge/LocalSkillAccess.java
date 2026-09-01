package com.skillport.bridge;

import com.skillport.protocol.EnvironmentPropertiesDocument;
import com.skillport.protocol.LocalSkillEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Locale;

public final class LocalSkillAccess {
    private static final int MAX_MANIFEST_BYTES = 512 * 1024;
    private static final int MAX_ENVIRONMENT_BYTES = 128 * 1024;

    private final Path home;
    private final DirectoryOpener directoryOpener;

    public LocalSkillAccess() {
        this(Path.of(System.getProperty("user.home")), LocalSkillAccess::openWithDesktop);
    }

    LocalSkillAccess(Path home, DirectoryOpener directoryOpener) {
        this.home = home.toAbsolutePath().normalize();
        this.directoryOpener = directoryOpener;
    }

    public void openFolder(String tool, String slug) throws IOException {
        directoryOpener.open(resolveSkillDirectory(tool, slug));
    }

    public String readManifest(String tool, String slug) throws IOException {
        Path skillDirectory = resolveSkillDirectory(tool, slug);
        Path manifest = findManifest(skillDirectory);
        if (manifest == null) throw new IOException("本机 Skill 中没有找到 SKILL.md");
        byte[] bytes;
        try (var input = Files.newInputStream(manifest)) {
            bytes = input.readNBytes(MAX_MANIFEST_BYTES + 1);
        }
        if (bytes.length > MAX_MANIFEST_BYTES) throw new IOException("SKILL.md 超过 512KB，无法预览");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public LocalSkillEnvironment readEnvironment(String tool, String slug) throws IOException {
        Path environment = environmentFile(resolveSkillDirectory(tool, slug));
        if (environment == null) return LocalSkillEnvironment.missing();
        String content = readLimited(environment, MAX_ENVIRONMENT_BYTES,
                "env.properties 超过 128KB，无法查看");
        try {
            EnvironmentPropertiesDocument document = EnvironmentPropertiesDocument.parse(content);
            return new LocalSkillEnvironment(true, environment.getFileName().toString(), document.values());
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    public LocalSkillEnvironment updateEnvironment(String tool, String slug,
                                                    Map<String, String> values) throws IOException {
        Path skillDirectory = resolveSkillDirectory(tool, slug);
        Path environment = environmentFile(skillDirectory);
        if (environment == null) throw new IOException("该 Skill 未包含 env.properties");
        String content = readLimited(environment, MAX_ENVIRONMENT_BYTES,
                "env.properties 超过 128KB，无法编辑");
        String updated;
        try {
            EnvironmentPropertiesDocument.validateUpdates(values);
            updated = EnvironmentPropertiesDocument.parse(content).updateValues(values);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
        Path temporary = Files.createTempFile(skillDirectory, ".skillport-env-", ".tmp");
        try {
            Files.writeString(temporary, updated, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, environment, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, environment, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return readEnvironment(tool, slug);
    }

    private Path resolveSkillDirectory(String tool, String slug) throws IOException {
        String literalSlug;
        try {
            literalSlug = ToolTargetPaths.literalSlug(slug);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
        Path root;
        try {
            root = ToolTargetPaths.root(home, normalizeTool(tool));
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
        Path directory = root.resolve(literalSlug).normalize();
        if (!directory.startsWith(root) || !directory.getParent().equals(root)
                || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("本机 Skill 目录不存在或不安全，请重新识别");
        }
        return directory;
    }

    private static Path findManifest(Path skillDirectory) throws IOException {
        try (var paths = Files.find(skillDirectory, 3,
                (path, attributes) -> attributes.isRegularFile()
                        && path.getFileName().toString().equalsIgnoreCase("SKILL.md"))) {
            return paths.findFirst().orElse(null);
        }
    }

    private static Path environmentFile(Path skillDirectory) throws IOException {
        Path manifest = findManifest(skillDirectory);
        if (manifest == null) throw new IOException("本机 Skill 中没有找到 SKILL.md");
        Path root = manifest.getParent();
        try (var children = Files.list(root)) {
            return children
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("env.properties"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static String readLimited(Path file, int maximumBytes, String errorMessage) throws IOException {
        byte[] bytes;
        try (var input = Files.newInputStream(file)) {
            bytes = input.readNBytes(maximumBytes + 1);
        }
        if (bytes.length > maximumBytes) throw new IOException(errorMessage);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String normalizeTool(String tool) {
        return tool == null ? "" : tool.trim().toLowerCase(Locale.ROOT);
    }

    private static void openWithDesktop(Path directory) throws IOException {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder builder;
        if (operatingSystem.contains("mac")) {
            builder = new ProcessBuilder("open", directory.toString());
        } else if (operatingSystem.contains("win")) {
            builder = new ProcessBuilder("explorer.exe", directory.toString());
        } else {
            throw new IOException("当前系统暂不支持打开本地文件夹");
        }
        builder.redirectErrorStream(true).start();
    }

    @FunctionalInterface
    interface DirectoryOpener {
        void open(Path directory) throws IOException;
    }
}
