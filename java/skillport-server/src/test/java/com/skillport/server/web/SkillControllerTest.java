package com.skillport.server.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillControllerTest {
    @Test
    void preservesArchiveExtensionsUsedBySkillInstallers() {
        assertEquals("zip", SkillController.fileExtension("release-audit.zip"));
        assertEquals("skill", SkillController.fileExtension("release-audit.skill"));
        assertEquals("md", SkillController.fileExtension("SKILL.md"));
        assertEquals("md", SkillController.fileExtension("notes.txt"));
    }
}
