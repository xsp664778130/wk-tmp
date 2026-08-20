package com.skillport.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "skillport")
public record SkillPortProperties(
        String gatewayKey,
        Duration sessionTtl,
        Path storageRoot,
        String publicApiBaseUrl,
        String publicNettyBaseUrl,
        Netty netty
) {
    public record Netty(int port, int workerThreads) {
    }
}
