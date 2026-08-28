package com.skillport.bridge;

import com.skillport.protocol.LocalSkillInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalSkillScannerTest {
    @TempDir
    Path home;

    @Test
    void readsManifestMetadataAndSkillPortOriginFromDetectedTools() throws Exception {
        Path installed = Files.createDirectories(home.resolve(".codex/skills/DMS_Audit"));
        Files.writeString(installed.resolve("SKILL.md"), """
                ---
                name: DMS 排查助手
                description: 查询数据库问题并整理证据
                ---

                # Instructions
                """);
        Files.writeString(installed.resolve(LocalSkillScanner.ORIGIN_MARKER), "skill-123");

        List<LocalSkillInfo> skills = new LocalSkillScanner(home).scan(List.of("codex"));

        assertEquals(1, skills.size());
        LocalSkillInfo skill = skills.getFirst();
        assertEquals("codex", skill.tool());
        assertEquals("DMS_Audit", skill.slug());
        assertEquals("DMS 排查助手", skill.name());
        assertEquals("查询数据库问题并整理证据", skill.description());
        assertEquals("~/.codex/skills/DMS_Audit", skill.relativePath());
        assertEquals("skill-123", skill.originSkillId());
    }

    @Test
    void ignoresUndetectedToolsAndKeepsExternalSkillsUnmarked() throws Exception {
        Path codex = Files.createDirectories(home.resolve(".codex/skills/external"));
        Files.writeString(codex.resolve("skill.md"), "---\nname: External\n---\n");
        Path qoder = Files.createDirectories(home.resolve(".qoder/skills/hidden"));
        Files.writeString(qoder.resolve("SKILL.md"), "---\nname: Hidden\n---\n");

        List<LocalSkillInfo> skills = new LocalSkillScanner(home).scan(List.of("codex"));

        assertEquals(1, skills.size());
        assertEquals("external", skills.getFirst().slug());
        assertNull(skills.getFirst().originSkillId());
    }
}
