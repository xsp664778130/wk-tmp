package com.skillport.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public final class ProtocolCodec {
    private final ObjectMapper objectMapper;

    public ProtocolCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(MessageType type, String requestId, Object payload) {
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        try {
            return objectMapper.writeValueAsString(new BridgeEnvelope(type, requestId, Instant.now(), payloadNode));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to encode bridge message", exception);
        }
    }

    public BridgeEnvelope decode(String json) {
        try {
            return objectMapper.readValue(json, BridgeEnvelope.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid bridge message", exception);
        }
    }

    public <T> T payload(BridgeEnvelope envelope, Class<T> payloadType) {
        try {
            return objectMapper.treeToValue(envelope.payload(), payloadType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid bridge payload", exception);
        }
    }
}
