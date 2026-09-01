package com.skillport.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
        Files.writeString(skill.resolve("env.properties"), "# local settings\nAPI_URL = https://old.example\nTOKEN=demo\n");
        AtomicReference<Path> opened = new AtomicReference<>();
        LocalSkillAccess access = new LocalSkillAccess(home, opened::set);

        assertEquals("---\nname: Demo\n---\n\nUse it safely.\n", access.readManifest("codex", "demo"));
        access.openFolder("codex", "demo");
        assertEquals(skill, opened.get());
        assertEquals(Map.of("API_URL", "https://old.example", "TOKEN", "demo"),
                access.readEnvironment("codex", "demo").values());

        access.updateEnvironment("codex", "demo", Map.of("API_URL", "https://new.example"));
        assertEquals("# local settings\nAPI_URL = https://new.example\nTOKEN=demo\n",
                Files.readString(skill.resolve("env.properties")));
    }

    @Test
    void rejectsTraversalAndUnknownDirectories() {
        LocalSkillAccess access = new LocalSkillAccess(home, ignored -> { });

        assertThrows(IOException.class, () -> access.readManifest("codex", "../outside"));
        assertThrows(IOException.class, () -> access.openFolder("unknown", "demo"));
        assertThrows(IOException.class, () -> access.openFolder("codex", "missing"));
    }

    @Test
    void reportsMissingEnvironmentAndRejectsUnknownKeys() throws Exception {
        Path skill = home.resolve(".codex/skills/demo");
        Files.createDirectories(skill);
        Files.writeString(skill.resolve("SKILL.md"), "---\nname: Demo\n---\n");
        LocalSkillAccess access = new LocalSkillAccess(home, ignored -> { });

        assertEquals(false, access.readEnvironment("codex", "demo").exists());
        assertThrows(IOException.class,
                () -> access.updateEnvironment("codex", "demo", Map.of("NEW_KEY", "value")));
    }
}
