package com.skillport.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "skillport")
public record SkillPortProperties(
        String gatewayKey,
        Duration sessionTtl,
        Path storageRoot,
        Path bridgeArtifactRoot,
        String publicApiBaseUrl,
        String publicNettyBaseUrl,
        ClientRelease clientRelease,
        WeCom wecom,
        Netty netty
) {
    public record ClientRelease(
            String version,
            String date,
            String title,
            List<String> changes,
            String downloadBaseUrl
    ) {
    }

    public record WeCom(boolean enabled, String corpId, String agentId, String secret, String callbackUrl) {
        public boolean configured() {
            return enabled
                    && corpId != null && !corpId.isBlank()
                    && agentId != null && !agentId.isBlank()
                    && secret != null && !secret.isBlank()
                    && callbackUrl != null && !callbackUrl.isBlank();
        }
    }

    public record Netty(int port, int workerThreads) {
    }
}
