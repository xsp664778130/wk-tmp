package com.skillport.server.web;

import com.skillport.server.config.SkillPortProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ClientReleaseController {
    private final SkillPortProperties properties;

    public ClientReleaseController(SkillPortProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/bridge/client/latest.json")
    public ResponseEntity<ClientReleaseResponse> latestRelease() {
        SkillPortProperties.ClientRelease release = properties.clientRelease();
        String baseUrl = release.downloadBaseUrl().replaceAll("/+$", "");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(new ClientReleaseResponse(
                        release.version(),
                        release.date(),
                        release.title(),
                        List.copyOf(release.changes()),
                        baseUrl + "/bridge/client/SkillPort-Bridge.pkg?v=" + release.version(),
                        baseUrl + "/bridge/client/SkillPort-Setup.exe?v=" + release.version()
                ));
    }

    public record ClientReleaseResponse(
            String version,
            String date,
            String title,
            List<String> changes,
            String macosUrl,
            String windowsUrl
    ) {
    }
}
