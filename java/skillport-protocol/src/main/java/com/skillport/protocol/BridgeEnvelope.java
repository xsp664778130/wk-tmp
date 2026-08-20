package com.skillport.protocol;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record BridgeEnvelope(
        MessageType type,
        String requestId,
        Instant timestamp,
        JsonNode payload
) {
}
