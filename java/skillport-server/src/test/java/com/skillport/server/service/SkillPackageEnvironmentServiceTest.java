package com.skillport.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillPackageEnvironmentServiceTest {
    @TempDir
    Path temporary;

    @Test
    void readsAndRewritesEnvironmentNextToManifest() throws Exception {
        Path archive = archive(Map.of(
                "demo/SKILL.md", "---\nname: Demo\ndescription: Demo skill\n---\n",
                "demo/env.properties", "# config\nAPI_URL=https://old.example\nTOKEN=demo\n",
                "demo/scripts/check.sh", "echo ok\n"));
        SkillPackageEnvironmentService service = new SkillPackageEnvironmentService();

        assertEquals(Map.of("API_URL", "https://old.example", "TOKEN", "demo"),
                service.read(archive, "demo.zip").values());

        Path rewritten = service.rewrite(archive, "demo.zip", Map.of("TOKEN", "changed"));
        try {
            assertEquals("# config\nAPI_URL=https://old.example\nTOKEN=changed\n",
                    entry(rewritten, "demo/env.properties"));
            assertEquals("echo ok\n", entry(rewritten, "demo/scripts/check.sh"));
        } finally {
            Files.deleteIfExists(rewritten);
        }
    }

    @Test
    void reportsMissingEnvironmentAndRejectsUnknownKey() throws Exception {
        Path archive = archive(Map.of(
                "SKILL.md", "---\nname: Demo\ndescription: Demo skill\n---\n"));
        SkillPackageEnvironmentService service = new SkillPackageEnvironmentService();

        assertFalse(service.read(archive, "demo.zip").exists());
        assertThrows(IllegalArgumentException.class,
                () -> service.rewrite(archive, "demo.zip", Map.of("TOKEN", "value")));
    }

    private Path archive(Map<String, String> entries) throws IOException {
        Path archive = temporary.resolve("demo.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return archive;
    }

    private static String entry(Path archive, String name) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.getName().equals(name)) return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("missing entry " + name);
    }
}
