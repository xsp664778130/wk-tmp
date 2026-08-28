package com.skillport.bridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;

public final class LocalSkillAccess {
    private static final int MAX_MANIFEST_BYTES = 512 * 1024;

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
