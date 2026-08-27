package com.skillport.server.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillPackageValidatorTest {
    private final SkillPackageValidator validator = new SkillPackageValidator();

    @Test
    void usesArchiveFileNameAsDisplayNameAndAcceptsOneSkillRoot() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("internal-skill/SKILL.md", manifest("internal-name"));
        entries.put("internal-skill/scripts/check.sh", "#!/bin/sh\necho ok\n".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file = archive("客户发布检查.zip", entries);

        SkillPackageValidator.SkillPackageMetadata metadata = validator.validate(file);

        assertEquals("客户发布检查", metadata.displayName());
        assertEquals("A valid test Skill", metadata.description());
    }

    @Test
    void acceptsAStandaloneSkillManifestAndUsesItsManifestName() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "SKILL.md", "text/markdown", manifest("standalone-skill"));

        SkillPackageValidator.SkillPackageMetadata metadata = validator.validate(file);

        assertEquals("standalone-skill", metadata.displayName());
    }

    @Test
    void acceptsLowercaseManifestAndIgnoresMacOsMetadata() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("lowercase-skill/skill.md", manifest("lowercase-skill"));
        entries.put("lowercase-skill/.DS_Store", new byte[]{0});
        entries.put("__MACOSX/lowercase-skill/._skill.md", new byte[]{0});
        MockMultipartFile file = archive("lowercase.zip", entries);

        SkillPackageValidator.SkillPackageMetadata metadata = validator.validate(file);

        assertEquals("lowercase", metadata.displayName());
        assertEquals("A valid test Skill", metadata.description());
    }

    @Test
    void rejectsArchiveWithoutManifest() throws IOException {
        MockMultipartFile file = archive("missing.zip", Map.of(
                "missing/readme.md", "not a skill".getBytes(StandardCharsets.UTF_8)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> validator.validate(file));

        assertEquals("Skill 结构不符合标准：压缩包中缺少 SKILL.md；已检查文件：missing/readme.md",
                exception.getReason());
    }

    @Test
    void rejectsMultipleSkillManifests() throws IOException {
        MockMultipartFile file = archive("multiple.zip", Map.of(
                "one/SKILL.md", manifest("one"),
                "two/SKILL.md", manifest("two")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> validator.validate(file));

        assertEquals("Skill 结构不符合标准：压缩包必须且只能包含一个 SKILL.md", exception.getReason());
    }

    @Test
    void rejectsFilesOutsideTheSkillRoot() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("valid/SKILL.md", manifest("valid"));
        entries.put("outside.txt", "outside".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file = archive("outside-root.zip", entries);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> validator.validate(file));

        assertEquals("Skill 结构不符合标准：所有文件必须与 SKILL.md 位于同一个 Skill 根目录内",
                exception.getReason());
    }

    @Test
    void rejectsTraversalEntries() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("safe/SKILL.md", manifest("safe"));
        entries.put("../escape.txt", "bad".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file = archive("traversal.zip", entries);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> validator.validate(file));

        assertEquals("Skill 结构不符合标准：压缩包包含越界文件路径", exception.getReason());
    }

    @Test
    void rejectsManifestWithoutRequiredDescription() {
        byte[] content = "---\nname: missing-description\n---\n\n# Instructions"
                .getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "SKILL.md", "text/markdown", content);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> validator.validate(file));

        assertEquals("Skill 结构不符合标准：frontmatter.description 不能为空", exception.getReason());
    }

    private static byte[] manifest(String name) {
        return ("---\nname: " + name + "\ndescription: A valid test Skill\n---\n"
                + "\n# Instructions\n\nFollow the requested workflow.\n").getBytes(StandardCharsets.UTF_8);
    }

    private static MockMultipartFile archive(String fileName, Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("file", fileName, "application/zip", output.toByteArray());
    }
}
