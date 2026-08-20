package com.skillport.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertEquals(tempDir.resolve(".openai/skills/demo"), ToolTargetPaths.resolve(tempDir, "openai", "demo"));
    }

    @Test
    void rejectsZipSlipEntries() throws IOException {
        Path archive = tempDir.resolve("bad.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../outside.txt"));
            zip.write("bad".getBytes());
            zip.closeEntry();
        }
        assertThrows(IOException.class, () -> SkillInstaller.unzipSafely(archive, tempDir.resolve("target")));
        assertFalse(Files.exists(tempDir.resolve("outside.txt")));
    }
}
