package com.skillport.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.protocol.MessageType;
import com.skillport.protocol.ProtocolCodec;
import com.skillport.protocol.ToolScanRequest;
import com.skillport.server.netty.BridgeSessionRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeviceToolScanService {
    private final DeviceService deviceService;
    private final BridgeSessionRegistry sessionRegistry;
    private final ProtocolCodec protocolCodec;

    public DeviceToolScanService(DeviceService deviceService, BridgeSessionRegistry sessionRegistry,
                                 ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.sessionRegistry = sessionRegistry;
        this.protocolCodec = new ProtocolCodec(objectMapper);
    }

    public ScanRequest request(String ownerId, String deviceId) {
        deviceService.ownedDevice(ownerId, deviceId);
        ScanRequest request = dispatch(deviceId, "user_refresh");
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bridge 当前离线，无法识别本机工具");
        }
        return request;
    }

    public void requestAfterConnect(String deviceId) {
        dispatch(deviceId, "bridge_connected");
    }

    private ScanRequest dispatch(String deviceId, String reason) {
        String requestId = UUID.randomUUID().toString();
        Instant requestedAt = Instant.now();
        String message = protocolCodec.encode(MessageType.SCAN_TOOLS, requestId, new ToolScanRequest(reason));
        return sessionRegistry.send(deviceId, message)
                ? new ScanRequest(deviceId, requestId, requestedAt)
                : null;
    }

    public record ScanRequest(String deviceId, String requestId, Instant requestedAt) {
    }
}
