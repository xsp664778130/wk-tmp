package com.skillport.server.web;

import com.skillport.server.config.SkillPortProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@RestController
@RequestMapping("/bridge")
public class BridgeArtifactController {
    private final Path artifactRoot;

    public BridgeArtifactController(SkillPortProperties properties) {
        this.artifactRoot = properties.bridgeArtifactRoot().toAbsolutePath().normalize();
    }

    @GetMapping("/skillport-bridge.jar")
    public ResponseEntity<Resource> bridgeJar() throws IOException {
        Path jar = artifact("skillport-bridge.jar");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/java-archive"))
                .contentLength(Files.size(jar))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"skillport-bridge.jar\"")
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(new FileSystemResource(jar));
    }

    @GetMapping(value = "/skillport-bridge.jar.sha256", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Resource> bridgeChecksum() {
        Path checksum = artifact("skillport-bridge.jar.sha256");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(new FileSystemResource(checksum));
    }

    private Path artifact(String fileName) {
        Path path = artifactRoot.resolve(fileName).normalize();
        if (!path.getParent().equals(artifactRoot) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Bridge 安装资源尚未准备好");
        }
        return path;
    }
}
