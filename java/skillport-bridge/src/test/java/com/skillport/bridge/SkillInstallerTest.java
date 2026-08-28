package com.skillport.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SkillInstallerTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesAllSupportedToolDirectories() {
        assertEquals(tempDir.resolve(".codex/skills/demo"), ToolTargetPaths.resolve(tempDir, "codex", "demo"));
        assertEquals(tempDir.resolve(".qoder/skills/demo"), ToolTargetPaths.resolve(tempDir, "qoder", "demo"));
        assertEquals(tempDir.resolve(".config/opencode/skills/demo"),
                ToolTargetPaths.resolve(tempDir, "opencode", "demo"));
        assertEquals(tempDir.resolve(".claude/skills/demo"), ToolTargetPaths.resolve(tempDir, "claude", "demo"));
        assertEquals(tempDir.resolve(".cursor/skills/demo"), ToolTargetPaths.resolve(tempDir, "cursor", "demo"));
    }

    @Test
    void rejectsZipSlipEntries() throws IOException {
        Path archive = tempDir.resolve("bad.zip");
        writeArchive(archive, Map.of("../outside.txt", "bad"));
        assertThrows(IOException.class, () -> SkillInstaller.unzipSafely(archive, tempDir.resolve("target")));
        assertFalse(Files.exists(tempDir.resolve("outside.txt")));
    }

    @Test
    void installsSkillWrappedInTopLevelDirectory() throws IOException {
        Path archive = tempDir.resolve("wrapped.zip");
        writeArchive(archive, Map.of(
                "internal-api-doc-sync/SKILL.md", "---\nname: internal-api-doc-sync\n---\n说明",
                "internal-api-doc-sync/scripts/sync.py", "print('ok')",
                "__MACOSX/internal-api-doc-sync/._SKILL.md", "metadata",
                "__MACOSX/internal-api-doc-sync/._sync.py", "metadata"
        ));

        Path destination = tempDir.resolve("skills/internal-api-doc-sync");
        SkillInstaller.installArchive(archive, destination);

        assertTrue(Files.isRegularFile(destination.resolve("SKILL.md")));
        assertTrue(Files.isRegularFile(destination.resolve("scripts/sync.py")));
        assertFalse(Files.exists(destination.resolve("internal-api-doc-sync")));
        assertFalse(Files.exists(destination.resolve("__MACOSX")));
    }

    @Test
    void installsSkillWhoseManifestIsAtArchiveRoot() throws IOException {
        Path archive = tempDir.resolve("root.zip");
        writeArchive(archive, Map.of(
                "SKILL.md", "---\nname: root-skill\n---\n说明",
                "references/guide.md", "guide"
        ));

        Path destination = tempDir.resolve("skills/root-skill");
        SkillInstaller.installArchive(archive, destination);

        assertTrue(Files.isRegularFile(destination.resolve("SKILL.md")));
        assertEquals("guide", Files.readString(destination.resolve("references/guide.md")));
    }

    @Test
    void normalizesLowercaseManifestAndBacksUpExistingInstallation() throws IOException {
        Path archive = tempDir.resolve("lowercase.zip");
        writeArchive(archive, Map.of(
                "lowercase-skill/skill.md", "---\nname: lowercase-skill\n---\nnew",
                "lowercase-skill/.DS_Store", "metadata"
        ));
        Path destination = tempDir.resolve("skills/lowercase-skill");
        Files.createDirectories(destination);
        Files.writeString(destination.resolve("old.txt"), "old");

        SkillInstaller.installArchive(archive, destination);

        assertTrue(Files.isRegularFile(destination.resolve("SKILL.md")));
        try (var files = Files.list(destination)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().equals("skill.md")));
        }
        assertFalse(Files.exists(destination.resolve(".DS_Store")));
        try (var siblings = Files.list(destination.getParent())) {
            Path backup = siblings
                    .filter(path -> path.getFileName().toString().startsWith("lowercase-skill.skillport-backup-"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("old", Files.readString(backup.resolve("old.txt")));
        }
    }

    @Test
    void rejectsArchiveWithMultipleSkillManifests() throws IOException {
        Path archive = tempDir.resolve("multiple.zip");
        writeArchive(archive, Map.of(
                "first/SKILL.md", "first",
                "second/SKILL.md", "second"
        ));

        IOException exception = assertThrows(IOException.class,
                () -> SkillInstaller.installArchive(archive, tempDir.resolve("skills/multiple")));
        assertTrue(exception.getMessage().contains("只能包含一个 SKILL.md"));
    }

    private static void writeArchive(Path archive, Map<String, String> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
