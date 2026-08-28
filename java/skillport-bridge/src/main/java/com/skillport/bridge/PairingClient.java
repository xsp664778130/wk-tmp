package com.skillport.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class PairingClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PairingClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public PairResult pair(String apiBaseUrl, String code, String name, String clientInstanceId) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "code", code,
                    "name", name,
                    "os", normalizedOs(),
                    "arch", System.getProperty("os.arch", "unknown"),
                    "clientInstanceId", clientInstanceId));
            HttpRequest request = HttpRequest.newBuilder(URI.create(trimSlash(apiBaseUrl) + "/api/v1/bridge/pair"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("配对失败，服务端状态码=" + response.statusCode());
            }
            return objectMapper.readValue(response.body(), PairResult.class);
        } catch (IOException exception) {
            throw new IllegalStateException("无法连接配对服务", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("配对请求被中断", exception);
        }
    }

    private static String normalizedOs() {
        String os = System.getProperty("os.name", "unknown").toLowerCase();
        if (os.contains("mac")) return "macos";
        if (os.contains("win")) return "windows";
        return "linux";
    }

    private static String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record PairResult(String deviceId, String deviceToken) {
    }
}
