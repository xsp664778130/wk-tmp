package com.skillport.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolDetectorTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsCommandsAndDesktopApplicationsOnMacOs() throws IOException {
        Path binaries = Files.createDirectories(tempDir.resolve("bin"));
        Files.createFile(binaries.resolve("codex"));
        Files.createFile(binaries.resolve("opencode"));
        Files.createFile(binaries.resolve("claude"));
        Files.createFile(binaries.resolve("cursor"));
        Files.createDirectories(tempDir.resolve("Applications/Qoder.app"));

        ToolDetector detector = new ToolDetector(tempDir, "Mac OS X", Map.of("PATH", binaries.toString()),
                tempDir.resolve("SystemApplications"));

        assertEquals(List.of("codex", "qoder", "opencode", "claude", "cursor"), detector.detect());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Qoder.app", "Qoder IDE.app", "Qoder CN.app"})
    void detectsEverySupportedQoderMacDistribution(String applicationName) throws IOException {
        Files.createDirectories(tempDir.resolve("SystemApplications").resolve(applicationName));

        ToolDetector detector = new ToolDetector(tempDir, "Mac OS X", Map.of("PATH", ""),
                tempDir.resolve("SystemApplications"));

        assertEquals(List.of("qoder"), detector.detect());
    }

    @Test
    void doesNotTreatSkillTargetDirectoriesAsInstalledTools() throws IOException {
        Files.createDirectories(tempDir.resolve(".codex/skills/example"));
        Files.createDirectories(tempDir.resolve(".qoder/skills/example"));
        Files.createDirectories(tempDir.resolve(".config/opencode/skills/example"));
        Files.createDirectories(tempDir.resolve(".claude/skills/example"));
        Files.createDirectories(tempDir.resolve(".cursor/skills/example"));

        ToolDetector detector = new ToolDetector(tempDir, "Mac OS X", Map.of("PATH", ""),
                tempDir.resolve("SystemApplications"));

        assertEquals(List.of(), detector.detect());
    }

    @Test
    void detectsWindowsCommandExtensionsCaseInsensitively() throws IOException {
        Path binaries = Files.createDirectories(tempDir.resolve("bin"));
        Files.createFile(binaries.resolve("qoder.cmd"));

        ToolDetector detector = new ToolDetector(tempDir, "Windows 11", Map.of("Path", binaries.toString()),
                tempDir.resolve("SystemApplications"));

        assertEquals(List.of("qoder"), detector.detect());
    }

    @Test
    void detectsQoderCnWhenWindowsUsesTheGenericExecutableName() throws IOException {
        Path localAppData = Files.createDirectories(tempDir.resolve("LocalAppData"));
        Path installation = Files.createDirectories(localAppData.resolve("Programs/Qoder CN"));
        Files.createFile(installation.resolve("Qoder.exe"));

        ToolDetector detector = new ToolDetector(tempDir, "Windows 11",
                Map.of("LOCALAPPDATA", localAppData.toString(), "Path", ""),
                tempDir.resolve("SystemApplications"));

        assertEquals(List.of("qoder"), detector.detect());
    }

    @Test
    void detectsCursorFromItsWindowsApplicationDirectory() throws IOException {
        Path localAppData = Files.createDirectories(tempDir.resolve("LocalAppData"));
        Path installation = Files.createDirectories(localAppData.resolve("Programs/Cursor"));
        Files.createFile(installation.resolve("Cursor.exe"));

        ToolDetector detector = new ToolDetector(tempDir, "Windows 11",
                Map.of("LOCALAPPDATA", localAppData.toString(), "Path", ""),
                tempDir.resolve("SystemApplications"));

        assertEquals(List.of("cursor"), detector.detect());
    }
}
