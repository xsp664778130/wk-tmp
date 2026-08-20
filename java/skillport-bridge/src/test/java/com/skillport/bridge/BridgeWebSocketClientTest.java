package com.skillport.bridge;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BridgeWebSocketClientTest {
    @Test
    void convertsHttpsPublicEndpointToSecureWebSocketEndpoint() {
        URI uri = BridgeWebSocketClient.bridgeUri("https://api.example.com", "device id", "token/value");

        assertEquals("wss", uri.getScheme());
        assertEquals("api.example.com", uri.getHost());
        assertEquals("/ws/bridge", uri.getPath());
        assertEquals("deviceId=device id&token=token/value", uri.getQuery());
    }
}
