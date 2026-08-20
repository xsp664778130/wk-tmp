package com.skillport.bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BridgeConfig(String apiBaseUrl, String nettyUrl, String deviceId, String deviceToken) {
    private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), ".skillport", "bridge.properties");

    public static BridgeConfig load() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Bridge 尚未配对，请先执行 pair 命令", exception);
        }
        return new BridgeConfig(required(properties, "apiBaseUrl"), required(properties, "nettyUrl"),
                required(properties, "deviceId"), required(properties, "deviceToken"));
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("apiBaseUrl", apiBaseUrl);
        properties.setProperty("nettyUrl", nettyUrl);
        properties.setProperty("deviceId", deviceId);
        properties.setProperty("deviceToken", deviceToken);
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                properties.store(output, "SkillPort Bridge configuration");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存 Bridge 配置", exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Bridge 配置缺少 " + key);
        return value.trim();
    }
}
