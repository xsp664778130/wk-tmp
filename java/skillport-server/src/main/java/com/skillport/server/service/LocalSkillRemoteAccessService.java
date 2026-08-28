package com.skillport.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.protocol.LocalSkillActionCommand;
import com.skillport.protocol.LocalSkillActionResult;
import com.skillport.protocol.MessageType;
import com.skillport.protocol.ProtocolCodec;
import com.skillport.server.domain.DeviceLocalSkillEntity;
import com.skillport.server.netty.BridgeSessionRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class LocalSkillRemoteAccessService {
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_PENDING_REQUESTS = 200;

    private final LocalSkillWorkspaceService workspaceService;
    private final BridgeSessionRegistry sessionRegistry;
    private final ProtocolCodec protocolCodec;
    private final ConcurrentMap<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    public LocalSkillRemoteAccessService(LocalSkillWorkspaceService workspaceService,
                                         BridgeSessionRegistry sessionRegistry,
                                         ObjectMapper objectMapper) {
        this.workspaceService = workspaceService;
        this.sessionRegistry = sessionRegistry;
        this.protocolCodec = new ProtocolCodec(objectMapper);
    }

    public LocalSkillActionResult openFolder(String ownerId, String deviceId, String tool, String slug) {
        return request(ownerId, deviceId, tool, slug, "OPEN_FOLDER", MessageType.OPEN_LOCAL_SKILL_FOLDER);
    }

    public LocalSkillActionResult readManifest(String ownerId, String deviceId, String tool, String slug) {
        return request(ownerId, deviceId, tool, slug, "READ_MANIFEST", MessageType.READ_LOCAL_SKILL_MANIFEST);
    }

    public void complete(String deviceId, String requestId, LocalSkillActionResult result) {
        PendingRequest pending = pendingRequests.get(requestId);
        if (pending == null || result == null || !pending.deviceId().equals(deviceId)
                || !pending.action().equals(result.action())
                || !pending.tool().equals(result.tool()) || !pending.slug().equals(result.slug())) {
            return;
        }
        pending.future().complete(result);
    }

    private LocalSkillActionResult request(String ownerId, String deviceId, String tool, String slug,
                                           String action, MessageType messageType) {
        if (pendingRequests.size() >= MAX_PENDING_REQUESTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "本机操作请求较多，请稍后重试");
        }
        DeviceLocalSkillEntity localSkill = workspaceService.ownedLocalSkill(ownerId, deviceId, tool, slug);
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<LocalSkillActionResult> future = new CompletableFuture<>();
        pendingRequests.put(requestId, new PendingRequest(
                deviceId, localSkill.getTool(), localSkill.getSlug(), action, future));
        try {
            String message = protocolCodec.encode(messageType, requestId,
                    new LocalSkillActionCommand(localSkill.getTool(), localSkill.getSlug()));
            if (!sessionRegistry.send(deviceId, message)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "本机 Bridge 不在线，请重新连接后再试");
            }
            LocalSkillActionResult result = future.get(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!result.success()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        result.message() == null ? "本机 Skill 操作失败" : result.message());
            }
            return result;
        } catch (TimeoutException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "等待本机 Bridge 响应超时", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "本机操作已中断", exception);
        } catch (ExecutionException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "本机 Bridge 响应失败", exception);
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    private record PendingRequest(String deviceId, String tool, String slug, String action,
                                  CompletableFuture<LocalSkillActionResult> future) {
    }
}
