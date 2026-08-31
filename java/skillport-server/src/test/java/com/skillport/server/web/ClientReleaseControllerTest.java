package com.skillport.server.web;

import com.skillport.server.config.SkillPortProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientReleaseControllerTest {
    @Test
    void returnsLatestReleaseAndPlatformSpecificDownloads() {
        SkillPortProperties properties = new SkillPortProperties(
                "gateway",
                Duration.ofDays(30),
                Path.of("data/skills"),
                Path.of("data/bridge"),
                "https://www.jmuyuer.com/",
                "https://bridge.jmuyuer.com",
                new SkillPortProperties.ClientRelease(
                        "1.0.18",
                        "2026-08-25",
                        "客户端在线更新",
                        List.of("自动检查新版本"),
                        "https://www.jmuyuer.com/"
                ),
                new SkillPortProperties.WeCom(false, "", "", "", ""),
                null,
                new SkillPortProperties.Netty(9091, 2)
        );

        ClientReleaseController.ClientReleaseResponse response =
                new ClientReleaseController(properties).latestRelease().getBody();

        assertEquals("1.0.18", response.version());
        assertEquals(List.of("自动检查新版本"), response.changes());
        assertEquals(
                "https://www.jmuyuer.com/bridge/client/SkillPort-Bridge.pkg?v=1.0.18",
                response.macosUrl()
        );
        assertEquals(
                "https://www.jmuyuer.com/bridge/client/SkillPort-Setup.exe?v=1.0.18",
                response.windowsUrl()
        );
    }
}
