package com.skillport.server.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.protocol.*;
import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.service.DeviceService;
import com.skillport.server.service.DeviceToolScanService;
import com.skillport.server.service.DownloadTicketService;
import com.skillport.server.service.InstallTaskService;
import com.skillport.server.service.LocalSkillWorkspaceService;
import com.skillport.server.service.LocalSkillRemoteAccessService;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.BYTES;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class SkillPortNettyHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(SkillPortNettyHandler.class);
    private static final int MAX_WEBSOCKET_FRAME_BYTES = 2 * 1024 * 1024;
    private static final AttributeKey<String> DEVICE_ID = AttributeKey.valueOf("skillport.deviceId");
    private final DeviceService deviceService;
    private final DownloadTicketService ticketService;
    private final InstallTaskService installTaskService;
    private final DeviceToolScanService toolScanService;
    private final LocalSkillWorkspaceService localSkillWorkspaceService;
    private final LocalSkillRemoteAccessService localSkillRemoteAccessService;
    private final BridgeSessionRegistry sessionRegistry;
    private final ProtocolCodec protocolCodec;

    public SkillPortNettyHandler(DeviceService deviceService, DownloadTicketService ticketService,
                                 InstallTaskService installTaskService, DeviceToolScanService toolScanService,
                                 LocalSkillWorkspaceService localSkillWorkspaceService,
                                 LocalSkillRemoteAccessService localSkillRemoteAccessService,
                                 BridgeSessionRegistry sessionRegistry,
                                 ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.ticketService = ticketService;
        this.installTaskService = installTaskService;
        this.toolScanService = toolScanService;
        this.localSkillWorkspaceService = localSkillWorkspaceService;
        this.localSkillRemoteAccessService = localSkillRemoteAccessService;
        this.sessionRegistry = sessionRegistry;
        this.protocolCodec = new ProtocolCodec(objectMapper);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof FullHttpRequest request) {
            handleHttp(context, request);
        } else if (message instanceof TextWebSocketFrame frame) {
            handleWebSocket(context, frame.text());
        } else if (message instanceof PingWebSocketFrame frame) {
            context.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (message instanceof CloseWebSocketFrame) {
            context.close();
        }
    }

    private void handleHttp(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (decoder.path().equals("/ws/bridge")) {
            handleWebSocketUpgrade(context, request, decoder);
            return;
        }
        if (request.method().equals(GET) && decoder.path().startsWith("/downloads/")) {
            handleDownload(context, request, decoder.path().substring("/downloads/".length()));
            return;
        }
        sendStatus(context, NOT_FOUND);
    }

    private void handleWebSocketUpgrade(ChannelHandlerContext context, FullHttpRequest request,
                                        QueryStringDecoder decoder) {
        String deviceId = first(decoder.parameters().get("deviceId"));
        String token = first(decoder.parameters().get("token"));
        Optional<DeviceEntity> authenticated = deviceService.authenticate(deviceId, token);
        if (authenticated.isEmpty()) {
            sendStatus(context, UNAUTHORIZED);
            return;
        }

        WebSocketServerHandshaker handshaker = new WebSocketServerHandshakerFactory(
                websocketLocation(request), null, true, MAX_WEBSOCKET_FRAME_BYTES).newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
            return;
        }
        handshaker.handshake(context.channel(), request).addListener(future -> {
            if (future.isSuccess()) {
                context.channel().attr(DEVICE_ID).set(deviceId);
                sessionRegistry.register(deviceId, context.channel());
                deviceService.markOnline(deviceId);
                toolScanService.requestAfterConnect(deviceId);
                log.info("Bridge connected deviceId={}", deviceId);
            }
        });
    }

    private void handleWebSocket(ChannelHandlerContext context, String json) {
        String deviceId = context.channel().attr(DEVICE_ID).get();
        if (deviceId == null) {
            context.close();
            return;
        }
        BridgeEnvelope envelope = protocolCodec.decode(json);
        switch (envelope.type()) {
            case HEARTBEAT -> {
                deviceService.heartbeat(deviceId);
                context.writeAndFlush(new TextWebSocketFrame(
                        protocolCodec.encode(MessageType.HEARTBEAT_ACK, envelope.requestId(), new HeartbeatPayload("ok"))));
            }
            case INSTALL_PROGRESS, INSTALL_COMPLETED, INSTALL_FAILED,
                    UNINSTALL_PROGRESS, UNINSTALL_COMPLETED, UNINSTALL_FAILED -> {
                InstallProgress progress = protocolCodec.payload(envelope, InstallProgress.class);
                boolean failed = envelope.type() == MessageType.INSTALL_FAILED
                        || envelope.type() == MessageType.UNINSTALL_FAILED;
                installTaskService.updateProgress(deviceId, progress, failed);
            }
            case TOOL_SCAN_RESULT -> {
                ToolScanResult result = protocolCodec.payload(envelope, ToolScanResult.class);
                localSkillWorkspaceService.replaceInventory(
                        deviceId, result.tools(), result.skills(), result.detectedAt());
            }
            case LOCAL_SKILL_ACTION_RESULT -> localSkillRemoteAccessService.complete(
                    deviceId, envelope.requestId(), protocolCodec.payload(envelope, LocalSkillActionResult.class));
            default -> log.warn("Ignored bridge message deviceId={} type={}", deviceId, envelope.type());
        }
    }

    private void handleDownload(ChannelHandlerContext context, FullHttpRequest request, String rawToken) throws Exception {
        DownloadTicketService.DownloadGrant grant = ticketService.resolve(rawToken);
        if (grant == null) {
            sendStatus(context, UNAUTHORIZED);
            return;
        }
        Range range = parseRange(request.headers().get(RANGE), grant.sizeBytes());
        if (range == null) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, REQUESTED_RANGE_NOT_SATISFIABLE);
            response.headers().set(CONTENT_RANGE, "bytes */" + grant.sizeBytes());
            context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        RandomAccessFile file = new RandomAccessFile(grant.path().toFile(), "r");
        HttpResponseStatus status = range.start() == 0 && range.length() == grant.sizeBytes() ? OK : PARTIAL_CONTENT;
        DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.headers().set(CONTENT_TYPE, grant.contentType());
        response.headers().set(CONTENT_LENGTH, range.length());
        response.headers().set(ACCEPT_RANGES, BYTES);
        response.headers().set("X-Skill-Sha256", grant.sha256());
        response.headers().set("X-Skill-Filename", grant.fileName());
        if (status.equals(PARTIAL_CONTENT)) {
            response.headers().set(CONTENT_RANGE,
                    "bytes " + range.start() + "-" + (range.start() + range.length() - 1) + "/" + grant.sizeBytes());
        }
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        context.write(response);
        ChannelFuture transfer = context.write(new ChunkedNioFile(file.getChannel(), range.start(), range.length(), 64 * 1024),
                context.newProgressivePromise());
        transfer.addListener(future -> file.close());
        ChannelFuture completed = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) completed.addListener(ChannelFutureListener.CLOSE);
    }

    private static Range parseRange(String value, long totalLength) {
        if (totalLength <= 0) return new Range(0, 0);
        if (value == null || value.isBlank()) return new Range(0, totalLength);
        if (!value.startsWith("bytes=") || value.contains(",")) return null;
        String[] bounds = value.substring(6).split("-", -1);
        try {
            long start = bounds[0].isBlank() ? 0 : Long.parseLong(bounds[0]);
            long end = bounds.length < 2 || bounds[1].isBlank() ? totalLength - 1 : Long.parseLong(bounds[1]);
            if (start < 0 || start >= totalLength || end < start) return null;
            end = Math.min(end, totalLength - 1);
            return new Range(start, end - start + 1);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void sendStatus(ChannelHandlerContext context, HttpResponseStatus status) {
        context.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, status)).addListener(ChannelFutureListener.CLOSE);
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private static String websocketLocation(FullHttpRequest request) {
        String host = request.headers().get(HOST, "localhost");
        return URI.create("ws://" + host + "/ws/bridge").toString();
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
        String deviceId = context.channel().attr(DEVICE_ID).get();
        if (deviceId != null) {
            sessionRegistry.unregister(deviceId, context.channel());
            if (!sessionRegistry.isOnline(deviceId)) deviceService.markOffline(deviceId);
            log.info("Bridge disconnected deviceId={}", deviceId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        String deviceId = context.channel().attr(DEVICE_ID).get();
        log.warn("Netty channel failed deviceId={} error={}", deviceId, cause.getMessage());
        context.close();
    }

    private record Range(long start, long length) {
    }
    private record HeartbeatPayload(String status) {
    }
}
