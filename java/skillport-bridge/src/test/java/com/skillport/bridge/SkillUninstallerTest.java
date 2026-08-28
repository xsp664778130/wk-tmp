package com.skillport.bridge;

import com.skillport.protocol.UninstallCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillUninstallerTest {
    @TempDir
    Path tempDir;

    @Test
    void permanentlyRemovesSelectedToolCopies() throws Exception {
        Path codexSkill = tempDir.resolve(".codex/skills/demo-skill");
        Path qoderSkill = tempDir.resolve(".qoder/skills/demo-skill");
        Files.createDirectories(codexSkill);
        Files.createDirectories(qoderSkill);
        Files.writeString(codexSkill.resolve("SKILL.md"), "codex");
        Files.writeString(qoderSkill.resolve("SKILL.md"), "qoder");
        List<String> stages = new ArrayList<>();
        SkillUninstaller uninstaller = new SkillUninstaller(
                (progress, stage, message) -> stages.add(stage), tempDir);

        SkillUninstaller.UninstallResult result = uninstaller.uninstall(new UninstallCommand(
                "task-123", "skill-123", "Demo Skill", List.of("codex", "qoder")));

        assertEquals(2, result.removedTargets());
        assertFalse(Files.exists(codexSkill));
        assertFalse(Files.exists(qoderSkill));
        assertTrue(stages.contains("UNINSTALLING"));
    }

    @Test
    void missingSkillIsAnIdempotentSuccess() {
        SkillUninstaller uninstaller = new SkillUninstaller((progress, stage, message) -> { }, tempDir);

        SkillUninstaller.UninstallResult result = uninstaller.uninstall(new UninstallCommand(
                "task-456", "skill-456", "Missing Skill", List.of("opencode")));

        assertEquals(0, result.removedTargets());
        assertEquals(1, result.requestedTargets());
    }

    @Test
    void removesTheExactScannedDirectoryForExternalSkills() throws Exception {
        Path externalSkill = tempDir.resolve(".cursor/skills/DMS_Audit.v2");
        Files.createDirectories(externalSkill);
        Files.writeString(externalSkill.resolve("SKILL.md"), "external");
        SkillUninstaller uninstaller = new SkillUninstaller((progress, stage, message) -> { }, tempDir);

        SkillUninstaller.UninstallResult result = uninstaller.uninstall(new UninstallCommand(
                "task-789", "skill-789", "Display Name", "DMS_Audit.v2", List.of("cursor")));

        assertEquals(1, result.removedTargets());
        assertFalse(Files.exists(externalSkill));
    }
}
