package com.skillport.bridge;

import com.skillport.protocol.InstallCommand;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

public class SkillInstaller {
    private final InstallProgressListener progressListener;
    private final Path home;

    public SkillInstaller(InstallProgressListener progressListener) {
        this(progressListener, Path.of(System.getProperty("user.home")));
    }

    SkillInstaller(InstallProgressListener progressListener, Path home) {
        this.progressListener = progressListener;
        this.home = home.toAbsolutePath().normalize();
    }

    public void install(InstallCommand command) {
        Path download = home.resolve(".skillport/downloads").resolve(command.taskId() + suffix(command.fileName()));
        progressListener.onProgress(1, "DOWNLOADING", "开始下载");
        Path completed = new ResumableNettyDownloader().download(
                command.downloadUrl(), download, command.sizeBytes(),
                progress -> progressListener.onProgress(progress, "DOWNLOADING", "正在下载"));
        progressListener.onProgress(92, "VERIFYING", "正在校验 SHA-256");
        verifySha256(completed, command.sha256());
        progressListener.onProgress(95, "INSTALLING", "正在写入工具目录");
        String slug = ToolTargetPaths.slug(command.skillName());
        for (String target : command.targets()) {
            Path destination = ToolTargetPaths.resolve(home, target, slug);
            installToTarget(completed, command.fileName(), destination);
            writeOriginMarker(destination, command.skillId());
        }
    }

    private static void writeOriginMarker(Path destination, String skillId) {
        try {
            Files.writeString(destination.resolve(LocalSkillScanner.ORIGIN_MARKER), skillId,
                    java.nio.charset.StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("无法记录 SkillPort 安装来源", exception);
        }
    }

    private static void installToTarget(Path source, String fileName, Path destination) {
        try {
            String lowerName = fileName.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".zip") || lowerName.endsWith(".skill")) {
                installArchive(source, destination);
            } else {
                Files.createDirectories(destination);
                Files.copy(source, destination.resolve("SKILL.md"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法安装到目录 " + destination, exception);
        }
    }

    static void installArchive(Path archive, Path destination) throws IOException {
        Path extractionDirectory = Files.createTempDirectory("skillport-extract-");
        Path destinationParent = destination.toAbsolutePath().normalize().getParent();
        if (destinationParent == null) throw new IOException("Skill 安装目录无效");
        Files.createDirectories(destinationParent);
        Path stage = Files.createTempDirectory(destinationParent, ".skillport-stage-");
        try {
            unzipSafely(archive, extractionDirectory);
            Path manifest = findSingleManifest(extractionDirectory);
            copySkillRoot(manifest.getParent(), manifest, stage);
            replaceDestination(stage, destination.toAbsolutePath().normalize());
        } finally {
            deleteTreeQuietly(stage);
            deleteTreeQuietly(extractionDirectory);
        }
    }

    private static Path findSingleManifest(Path extractionDirectory) throws IOException {
        List<Path> manifests;
        try (Stream<Path> paths = Files.walk(extractionDirectory)) {
            manifests = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                    .filter(path -> !isIgnoredMetadata(extractionDirectory.relativize(path)))
                    .limit(2)
                    .toList();
        }
        if (manifests.size() != 1) {
            throw new IOException("Skill 压缩包必须且只能包含一个 SKILL.md");
        }
        return manifests.getFirst();
    }

    private static void copySkillRoot(Path sourceRoot, Path manifest, Path destination) throws IOException {
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                Path relative = sourceRoot.relativize(directory);
                if (!relative.toString().isEmpty() && isIgnoredMetadata(relative)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(destination.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Path relative = sourceRoot.relativize(file);
                if (isIgnoredMetadata(relative)) return FileVisitResult.CONTINUE;
                Path targetRelative = file.equals(manifest)
                        ? Path.of("SKILL.md")
                        : relative;
                Path target = destination.resolve(targetRelative).normalize();
                if (!target.startsWith(destination)) throw new IOException("Skill 压缩包包含非法路径");
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isIgnoredMetadata(Path relative) {
        for (Path part : relative) {
            String name = part.toString();
            if (name.equalsIgnoreCase("__MACOSX") || name.equals(".DS_Store") || name.startsWith("._")) {
                return true;
            }
        }
        return false;
    }

    private static void replaceDestination(Path stage, Path destination) throws IOException {
        Path backup = destination.resolveSibling(destination.getFileName()
                + ".skillport-backup-" + System.currentTimeMillis() + "-" + UUID.randomUUID());
        boolean backedUp = false;
        try {
            if (Files.exists(destination)) {
                Files.move(destination, backup);
                backedUp = true;
            }
            moveDirectory(stage, destination);
        } catch (IOException exception) {
            if (backedUp && !Files.exists(destination)) {
                try {
                    Files.move(backup, destination);
                } catch (IOException restoreException) {
                    exception.addSuppressed(restoreException);
                }
            }
            throw exception;
        }
    }

    private static void moveDirectory(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private static void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                    if (exception != null) throw exception;
                    Files.deleteIfExists(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Temporary installation files are best-effort cleanup only.
        }
    }

    static void unzipSafely(Path archive, Path destination) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = normalizedDestination.resolve(entry.getName()).normalize();
                if (!target.startsWith(normalizedDestination)) throw new IOException("Skill 压缩包包含非法路径");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private static void verifySha256(Path file, String expected) {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!MessageDigest.isEqual(actual.getBytes(), expected.getBytes())) {
                throw new IllegalStateException("Skill 文件校验失败");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取下载文件", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static String suffix(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? ".zip" : fileName.substring(dot);
    }

    @FunctionalInterface
    public interface InstallProgressListener {
        void onProgress(int progress, String stage, String message);
    }
}
