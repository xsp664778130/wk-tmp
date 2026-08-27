package com.skillport.server.web;

import com.skillport.server.config.SkillPortProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BridgeArtifactControllerTest {
    @TempDir
    Path artifactRoot;

    @Test
    void servesAllowlistedNativeClientArtifactAsDownload() throws Exception {
        Path installer = artifactRoot.resolve("SkillPort-Setup.exe");
        Files.write(installer, new byte[]{1, 2, 3, 4});
        BridgeArtifactController controller = new BridgeArtifactController(properties());

        var response = controller.clientArtifact("SkillPort-Setup.exe");

        assertEquals(4, response.getHeaders().getContentLength());
        assertEquals("attachment; filename=\"SkillPort-Setup.exe\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertNotNull(response.getBody());
    }

    private SkillPortProperties properties() {
        return new SkillPortProperties("gateway", Duration.ofDays(30), artifactRoot, artifactRoot,
                "https://www.jmuyuer.com", "https://bridge.jmuyuer.com",
                new SkillPortProperties.ClientRelease("1.0.18", "2026-08-25", "Skill 详情操作区焕新",
                        List.of("检查新版本"), "https://www.jmuyuer.com"),
                new SkillPortProperties.WeCom(false, "", "", "", ""),
                new SkillPortProperties.Netty(9091, 2));
    }
}
