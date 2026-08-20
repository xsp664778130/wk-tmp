package com.skillport.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "skillport")
public record SkillPortProperties(
        String gatewayKey,
        Path storageRoot,
        String publicApiBaseUrl,
        String publicNettyBaseUrl,
        Netty netty
) {
    public record Netty(int port, int workerThreads) {
    }
}
