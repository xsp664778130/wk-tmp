package com.skillport.server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class SkillPackageValidator {
    private static final int MAX_ENTRIES = 512;
    private static final long MAX_UNCOMPRESSED_SIZE = 100L * 1024 * 1024;
    private static final int MAX_MANIFEST_SIZE = 512 * 1024;

    public SkillPackageMetadata validate(MultipartFile file) {
        String fileName = originalFileName(file);
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".zip") || lowerName.endsWith(".skill")) {
                ManifestMetadata manifest = validateArchive(file);
                return new SkillPackageMetadata(archiveDisplayName(fileName), manifest.description());
            }
            if (fileName.equalsIgnoreCase("SKILL.md")) {
                ManifestMetadata manifest = validateManifest(readLimited(
                        file.getInputStream(), MAX_MANIFEST_SIZE, "SKILL.md 不能超过 512KB"));
                return new SkillPackageMetadata(manifest.name(), manifest.description());
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid("压缩包无法读取或已经损坏");
        }
        throw invalid("仅支持 .zip、.skill，或单文件 SKILL.md");
    }

    private ManifestMetadata validateArchive(MultipartFile file) throws IOException {
        List<String> fileEntries = new ArrayList<>();
        ManifestMetadata manifest = null;
        String manifestPath = null;
        int entryCount = 0;
        long totalSize = 0;

        try (ZipInputStream zip = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String entryName = validateEntryName(entry.getName());
                if (!entry.isDirectory()) {
                    entryCount++;
                    if (entryCount > MAX_ENTRIES) {
                        throw invalid("压缩包文件数量不能超过 " + MAX_ENTRIES + " 个");
                    }
                    if (!isArchiveMetadata(entryName)) {
                        fileEntries.add(entryName);
                    }
                    boolean isManifest = isManifestPath(entryName);
                    EntryContent content = readEntry(zip, isManifest);
                    totalSize += content.size();
                    if (totalSize > MAX_UNCOMPRESSED_SIZE) {
                        throw invalid("解压后的文件总大小不能超过 100MB");
                    }
                    if (isManifest) {
                        if (manifest != null) {
                            throw invalid("压缩包必须且只能包含一个 SKILL.md");
                        }
                        manifest = validateManifest(content.bytes());
                        manifestPath = entryName;
                    }
                }
                zip.closeEntry();
            }
        }

        if (entryCount == 0) {
            throw invalid("压缩包不能为空");
        }
        if (manifest == null || manifestPath == null) {
            throw invalid("压缩包中缺少 SKILL.md；已检查文件：" + checkedEntries(fileEntries));
        }
        int lastSeparator = manifestPath.lastIndexOf('/');
        String root = lastSeparator < 0 ? "" : manifestPath.substring(0, lastSeparator);
        if (!root.isEmpty()) {
            String rootPrefix = root + "/";
            boolean outsideRoot = fileEntries.stream().anyMatch(name -> !name.startsWith(rootPrefix));
            if (outsideRoot) {
                throw invalid("所有文件必须与 SKILL.md 位于同一个 Skill 根目录内");
            }
        }
        return manifest;
    }

    private static boolean isManifestPath(String entryName) {
        int lastSeparator = entryName.lastIndexOf('/');
        String fileName = lastSeparator < 0 ? entryName : entryName.substring(lastSeparator + 1);
        return fileName.equalsIgnoreCase("SKILL.md");
    }

    private static boolean isArchiveMetadata(String entryName) {
        if (entryName.equals("__MACOSX") || entryName.startsWith("__MACOSX/")) {
            return true;
        }
        int lastSeparator = entryName.lastIndexOf('/');
        String fileName = lastSeparator < 0 ? entryName : entryName.substring(lastSeparator + 1);
        return fileName.equals(".DS_Store");
    }

    private static String checkedEntries(List<String> fileEntries) {
        if (fileEntries.isEmpty()) return "无可识别文件";
        String entries = fileEntries.stream()
                .limit(8)
                .map(name -> name.length() > 80 ? name.substring(0, 77) + "..." : name)
                .reduce((left, right) -> left + "、" + right)
                .orElse("无可识别文件");
        return fileEntries.size() > 8 ? entries + " 等 " + fileEntries.size() + " 个文件" : entries;
    }

    private static EntryContent readEntry(ZipInputStream zip, boolean capture) throws IOException {
        ByteArrayOutputStream output = capture ? new ByteArrayOutputStream() : null;
        byte[] buffer = new byte[8192];
        long size = 0;
        int read;
        while ((read = zip.read(buffer)) != -1) {
            size += read;
            if (capture && size > MAX_MANIFEST_SIZE) {
                throw invalid("SKILL.md 不能超过 512KB");
            }
            if (capture) {
                output.write(buffer, 0, read);
            }
            if (size > MAX_UNCOMPRESSED_SIZE) {
                throw invalid("单个文件解压后不能超过 100MB");
            }
        }
        return new EntryContent(size, capture ? output.toByteArray() : null);
    }

    private static byte[] readLimited(InputStream input, int limit, String errorMessage) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw invalid(errorMessage);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String validateEntryName(String value) {
        if (value == null || value.isBlank() || value.indexOf('\\') >= 0 || value.startsWith("/")
                || value.matches("^[A-Za-z]:.*")) {
            throw invalid("压缩包包含非法文件路径");
        }
        String[] segments = value.split("/");
        for (String segment : segments) {
            if (segment.equals("..") || segment.equals(".")) {
                throw invalid("压缩包包含越界文件路径");
            }
        }
        return value;
    }

    private static ManifestMetadata validateManifest(byte[] content) {
        String markdown;
        try {
            markdown = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw invalid("SKILL.md 必须使用 UTF-8 编码");
        }
        markdown = markdown.replace("\r\n", "\n").replace('\r', '\n');
        if (markdown.startsWith("\uFEFF")) {
            markdown = markdown.substring(1);
        }
        String[] lines = markdown.split("\n", -1);
        if (lines.length < 4 || !lines[0].trim().equals("---")) {
            throw invalid("SKILL.md 必须以 YAML frontmatter 开头");
        }
        int frontmatterEnd = -1;
        String name = "";
        String description = "";
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.equals("---")) {
                frontmatterEnd = index;
                break;
            }
            if (line.startsWith("name:")) {
                name = scalarValue(line.substring("name:".length()));
            } else if (line.startsWith("description:")) {
                description = scalarValue(line.substring("description:".length()));
            }
        }
        if (frontmatterEnd < 0) {
            throw invalid("SKILL.md 的 YAML frontmatter 缺少结束标记 ---");
        }
        if (!name.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || name.length() > 64) {
            throw invalid("frontmatter.name 必须是 1–64 位小写字母、数字或连字符");
        }
        if (description.isBlank()) {
            throw invalid("frontmatter.description 不能为空");
        }
        String body = String.join("\n", java.util.Arrays.copyOfRange(lines, frontmatterEnd + 1, lines.length));
        if (body.isBlank()) {
            throw invalid("SKILL.md 必须包含实际使用说明");
        }
        return new ManifestMetadata(name, description);
    }

    private static String scalarValue(String value) {
        String normalized = value.trim();
        if (normalized.length() >= 2
                && ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            return normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static String originalFileName(MultipartFile file) {
        String value = file.getOriginalFilename();
        if (value == null || value.isBlank()) {
            throw invalid("上传文件缺少文件名");
        }
        String normalized = value.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static String archiveDisplayName(String fileName) {
        String displayName = fileName.replaceFirst("(?i)\\.(zip|skill)$", "").trim();
        if (displayName.isBlank() || displayName.length() > 160) {
            throw invalid("压缩包文件名去掉扩展名后必须为 1–160 个字符");
        }
        return displayName;
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill 结构不符合标准：" + reason);
    }

    public record SkillPackageMetadata(String displayName, String description) {
    }

    private record ManifestMetadata(String name, String description) {
    }

    private record EntryContent(long size, byte[] bytes) {
    }
}
