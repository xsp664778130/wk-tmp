package com.skillport.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalSkillAccessTest {
    @TempDir
    Path home;

    @Test
    void readsManifestAndOpensOnlyAnIdentifiedSkillDirectory() throws Exception {
        Path skill = home.resolve(".codex/skills/demo");
        Files.createDirectories(skill);
        Files.writeString(skill.resolve("SKILL.md"), "---\nname: Demo\n---\n\nUse it safely.\n");
        AtomicReference<Path> opened = new AtomicReference<>();
        LocalSkillAccess access = new LocalSkillAccess(home, opened::set);

        assertEquals("---\nname: Demo\n---\n\nUse it safely.\n", access.readManifest("codex", "demo"));
        access.openFolder("codex", "demo");
        assertEquals(skill, opened.get());
    }

    @Test
    void rejectsTraversalAndUnknownDirectories() {
        LocalSkillAccess access = new LocalSkillAccess(home, ignored -> { });

        assertThrows(IOException.class, () -> access.readManifest("codex", "../outside"));
        assertThrows(IOException.class, () -> access.openFolder("unknown", "demo"));
        assertThrows(IOException.class, () -> access.openFolder("codex", "missing"));
    }
}
