package com.skillport.server.web;

import com.skillport.server.config.SkillPortProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

@RestController
@RequestMapping("/bridge")
public class BridgeArtifactController {
    private static final Set<String> RUNTIME_ARTIFACTS = Set.of(
            "temurin-jre21-windows-x64.zip",
            "temurin-jre21-windows-x64.zip.sha256",
            "temurin-jre21-windows-aarch64.zip",
            "temurin-jre21-windows-aarch64.zip.sha256",
            "temurin-jre21-macos-x64.tar.gz",
            "temurin-jre21-macos-x64.tar.gz.sha256",
            "temurin-jre21-macos-aarch64.tar.gz",
            "temurin-jre21-macos-aarch64.tar.gz.sha256"
    );
    private static final Set<String> CLIENT_ARTIFACTS = Set.of(
            "SkillPort-Setup.exe",
            "SkillPort-Bridge.pkg",
            "SHA256SUMS.txt"
    );

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

    @GetMapping("/runtime/{fileName}")
    public ResponseEntity<Resource> runtimeArtifact(@PathVariable String fileName) throws IOException {
        if (!RUNTIME_ARTIFACTS.contains(fileName)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Java 运行时资源不存在");
        }
        Path runtime = artifact(fileName);
        MediaType contentType = fileName.endsWith(".sha256")
                ? MediaType.TEXT_PLAIN
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(Files.size(runtime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
                .body(new FileSystemResource(runtime));
    }

    @GetMapping("/client/{fileName}")
    public ResponseEntity<Resource> clientArtifact(@PathVariable String fileName) throws IOException {
        if (!CLIENT_ARTIFACTS.contains(fileName)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "客户端安装资源不存在");
        }
        Path client = artifact(fileName);
        MediaType contentType = fileName.endsWith(".txt")
                ? MediaType.TEXT_PLAIN
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(Files.size(client))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache())
                .body(new FileSystemResource(client));
    }

    private Path artifact(String fileName) {
        Path path = artifactRoot.resolve(fileName).normalize();
        if (!path.getParent().equals(artifactRoot) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Bridge 安装资源尚未准备好");
        }
        return path;
    }
}
