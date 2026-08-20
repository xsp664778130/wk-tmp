package com.skillport.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.*;

public class BridgeWebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(BridgeWebSocketClient.class);
    private final BridgeConfig config;
    private final ProtocolCodec protocolCodec;
    private final ObjectMapper objectMapper;
    private final ExecutorService installExecutor = Executors.newFixedThreadPool(2, Thread.ofPlatform().name("skill-install-", 0).factory());

    public BridgeWebSocketClient(BridgeConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.protocolCodec = new ProtocolCodec(objectMapper);
    }

    public void runForever() {
        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    connect(group).closeFuture().sync();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception exception) {
                    log.warn("Bridge connection failed error={}", exception.getMessage());
                }
                sleep(Duration.ofSeconds(5));
            }
        } finally {
            installExecutor.shutdownNow();
            group.shutdownGracefully().syncUninterruptibly();
        }
    }

    private Channel connect(EventLoopGroup group) throws Exception {
        URI uri = bridgeUri();
        boolean ssl = uri.getScheme().equalsIgnoreCase("wss");
        int port = uri.getPort() > 0 ? uri.getPort() : ssl ? 443 : 80;
        SslContext sslContext = ssl ? SslContextBuilder.forClient().build() : null;
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), 64 * 1024);
        BridgeClientHandler handler = new BridgeClientHandler(handshaker);

        Channel channel = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        if (sslContext != null) channel.pipeline().addLast(sslContext.newHandler(channel.alloc(), uri.getHost(), port));
                        channel.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(64 * 1024))
                                .addLast(handler);
                    }
                })
                .connect(uri.getHost(), port).sync().channel();
        handler.handshakeFuture().sync();
        log.info("Bridge connected deviceId={}", config.deviceId());
        return channel;
    }

    private URI bridgeUri() {
        return bridgeUri(config.nettyUrl(), config.deviceId(), config.deviceToken());
    }

    static URI bridgeUri(String nettyUrl, String deviceId, String deviceToken) {
        String base = nettyUrl.endsWith("/") ? nettyUrl.substring(0, nettyUrl.length() - 1) : nettyUrl;
        URI baseUri = URI.create(base);
        String webSocketScheme = switch (baseUri.getScheme().toLowerCase()) {
            case "https", "wss" -> "wss";
            case "http", "ws" -> "ws";
            default -> throw new IllegalArgumentException("Netty URL 必须使用 http、https、ws 或 wss");
        };
        try {
            return new URI(webSocketScheme, baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(),
                    "/ws/bridge", "deviceId=" + deviceId + "&token=" + deviceToken, null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Netty URL 无效", exception);
        }
    }

    private void install(Channel channel, InstallCommand command) {
        installExecutor.execute(() -> {
            SkillInstaller installer = new SkillInstaller((progress, stage, message) ->
                    sendProgress(channel, command.taskId(), progress, stage, message, MessageType.INSTALL_PROGRESS));
            try {
                installer.install(command);
                sendProgress(channel, command.taskId(), 100, "COMPLETED", "安装完成", MessageType.INSTALL_COMPLETED);
            } catch (Exception exception) {
                log.warn("Skill installation failed taskId={} error={}", command.taskId(), exception.getMessage());
                sendProgress(channel, command.taskId(), 0, "FAILED", exception.getMessage(), MessageType.INSTALL_FAILED);
            }
        });
    }

    private void sendProgress(Channel channel, String taskId, int progress, String stage, String message, MessageType type) {
        if (!channel.isActive()) return;
        String json = protocolCodec.encode(type, taskId, new InstallProgress(taskId, progress, stage, message));
        channel.writeAndFlush(new TextWebSocketFrame(json));
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private final class BridgeClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;
        private ScheduledFuture<?> heartbeat;

        private BridgeClientHandler(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        ChannelFuture handshakeFuture() { return handshakeFuture; }

        @Override
        public void handlerAdded(ChannelHandlerContext context) {
            handshakeFuture = context.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext context) {
            handshaker.handshake(context.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, Object message) {
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(context.channel(), (FullHttpResponse) message);
                handshakeFuture.setSuccess();
                heartbeat = context.executor().scheduleAtFixedRate(() -> {
                    String id = Long.toString(System.currentTimeMillis());
                    context.writeAndFlush(new TextWebSocketFrame(
                            protocolCodec.encode(MessageType.HEARTBEAT, id, new Heartbeat("alive"))));
                }, 5, 30, TimeUnit.SECONDS);
                return;
            }
            if (message instanceof TextWebSocketFrame frame) {
                BridgeEnvelope envelope = protocolCodec.decode(frame.text());
                if (envelope.type() == MessageType.INSTALL_SKILL) {
                    install(context.channel(), protocolCodec.payload(envelope, InstallCommand.class));
                }
            } else if (message instanceof PingWebSocketFrame frame) {
                context.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            } else if (message instanceof CloseWebSocketFrame) {
                context.close();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            if (heartbeat != null) heartbeat.cancel(false);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            if (!handshakeFuture.isDone()) handshakeFuture.setFailure(cause);
            context.close();
        }
    }

    private record Heartbeat(String state) {
    }
}
