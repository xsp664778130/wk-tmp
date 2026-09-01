package com.skillport.server.service;

import com.skillport.protocol.EnvironmentPropertiesDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class SkillPackageEnvironmentService {
    private static final int MAX_ENVIRONMENT_BYTES = 128 * 1024;

    public EnvironmentView read(Path packageFile, String fileName) {
        if (!isArchive(fileName)) return EnvironmentView.missing();
        try {
            ArchiveLayout layout = inspect(packageFile);
            if (layout.environmentPath() == null) return EnvironmentView.missing();
            String content = readEnvironmentEntry(packageFile, layout.environmentPath());
            EnvironmentPropertiesDocument document = EnvironmentPropertiesDocument.parse(content);
            return new EnvironmentView(true, layout.environmentPath(), document.values());
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取 Skill 包中的 env.properties", exception);
        }
    }

    public Path rewrite(Path packageFile, String fileName, Map<String, String> values) {
        EnvironmentPropertiesDocument.validateUpdates(values);
        if (!isArchive(fileName)) throw new IllegalArgumentException("单文件 SKILL.md 不包含 env.properties");
        try {
            ArchiveLayout layout = inspect(packageFile);
            if (layout.environmentPath() == null) {
                throw new IllegalArgumentException("该 Skill 未包含 env.properties");
            }
            String current = readEnvironmentEntry(packageFile, layout.environmentPath());
            String updated = EnvironmentPropertiesDocument.parse(current).updateValues(values);
            Path temporary = Files.createTempFile("skillport-env-", ".zip");
            try {
                rewriteArchive(packageFile, temporary, layout.environmentPath(),
                        updated.getBytes(StandardCharsets.UTF_8));
                return temporary;
            } catch (RuntimeException | IOException exception) {
                Files.deleteIfExists(temporary);
                throw exception;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法更新 Skill 包中的 env.properties", exception);
        }
    }

    private static ArchiveLayout inspect(Path packageFile) throws IOException {
        String manifestPath = null;
        List<String> environmentPaths = new ArrayList<>();
        try (InputStream input = Files.newInputStream(packageFile);
             ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalizedEntryName(entry.getName());
                if (!entry.isDirectory() && fileName(name).equalsIgnoreCase("SKILL.md")) {
                    if (manifestPath != null) throw new IllegalArgumentException("Skill 包包含多个 SKILL.md");
                    manifestPath = name;
                }
                if (!entry.isDirectory() && fileName(name).equalsIgnoreCase("env.properties")) {
                    environmentPaths.add(name);
                }
                zip.closeEntry();
            }
        }
        if (manifestPath == null) throw new IllegalArgumentException("Skill 包中缺少 SKILL.md");
        String root = parent(manifestPath);
        List<String> matching = environmentPaths.stream()
                .filter(path -> parent(path).equalsIgnoreCase(root))
                .toList();
        if (matching.size() > 1) throw new IllegalArgumentException("Skill 根目录包含多个 env.properties");
        return new ArchiveLayout(matching.isEmpty() ? null : matching.getFirst());
    }

    private static String readEnvironmentEntry(Path packageFile, String environmentPath) throws IOException {
        try (InputStream input = Files.newInputStream(packageFile);
             ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalizedEntryName(entry.getName());
                if (!entry.isDirectory() && name.equals(environmentPath)) {
                    byte[] bytes = readLimited(zip, MAX_ENVIRONMENT_BYTES);
                    return new String(bytes, StandardCharsets.UTF_8);
                }
                zip.closeEntry();
            }
        }
        throw new IllegalArgumentException("该 Skill 未包含 env.properties");
    }

    private static void rewriteArchive(Path source, Path target, String environmentPath,
                                       byte[] updatedEnvironment) throws IOException {
        boolean replaced = false;
        try (InputStream input = Files.newInputStream(source);
             ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8);
             OutputStream output = Files.newOutputStream(target);
             ZipOutputStream rewritten = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalizedEntryName(entry.getName());
                ZipEntry copy = new ZipEntry(name);
                if (entry.getTime() >= 0) copy.setTime(entry.getTime());
                if (entry.getComment() != null) copy.setComment(entry.getComment());
                if (entry.getExtra() != null) copy.setExtra(entry.getExtra());
                rewritten.putNextEntry(copy);
                if (!entry.isDirectory() && name.equals(environmentPath)) {
                    rewritten.write(updatedEnvironment);
                    replaced = true;
                } else {
                    int read;
                    while ((read = zip.read(buffer)) != -1) rewritten.write(buffer, 0, read);
                }
                rewritten.closeEntry();
                zip.closeEntry();
            }
        }
        if (!replaced) throw new IllegalArgumentException("该 Skill 未包含 env.properties");
    }

    private static byte[] readLimited(InputStream input, int maximumBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maximumBytes) throw new IllegalArgumentException("env.properties 不能超过 128KB");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String normalizedEntryName(String name) {
        String normalized = name == null ? "" : name.replace('\\', '/');
        boolean traversesParent = List.of(normalized.split("/", -1)).contains("..");
        if (normalized.isBlank() || normalized.startsWith("/") || traversesParent
                || normalized.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("Skill 包包含不安全路径");
        }
        return normalized;
    }

    private static String parent(String path) {
        int separator = path.lastIndexOf('/');
        return separator < 0 ? "" : path.substring(0, separator);
    }

    private static String fileName(String path) {
        int separator = path.lastIndexOf('/');
        return separator < 0 ? path : path.substring(separator + 1);
    }

    private static boolean isArchive(String fileName) {
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".zip") || normalized.endsWith(".skill");
    }

    public record EnvironmentView(boolean exists, String path, Map<String, String> values) {
        static EnvironmentView missing() {
            return new EnvironmentView(false, "env.properties", Map.of());
        }
    }

    private record ArchiveLayout(String environmentPath) {
    }
}
