package com.skillport.bridge;

import com.skillport.protocol.InstallCommand;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            installToTarget(completed, command.fileName(), ToolTargetPaths.resolve(home, target, slug));
        }
    }

    private static void installToTarget(Path source, String fileName, Path destination) {
        try {
            Files.createDirectories(destination);
            String lowerName = fileName.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".zip") || lowerName.endsWith(".skill")) {
                unzipSafely(source, destination);
            } else {
                Files.copy(source, destination.resolve("SKILL.md"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法安装到目录 " + destination, exception);
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
