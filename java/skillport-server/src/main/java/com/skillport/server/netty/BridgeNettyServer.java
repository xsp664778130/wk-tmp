package com.skillport.server.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.service.DeviceService;
import com.skillport.server.service.DeviceToolScanService;
import com.skillport.server.service.DownloadTicketService;
import com.skillport.server.service.InstallTaskService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class BridgeNettyServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(BridgeNettyServer.class);
    private final SkillPortProperties properties;
    private final DeviceService deviceService;
    private final DownloadTicketService downloadTicketService;
    private final InstallTaskService installTaskService;
    private final DeviceToolScanService toolScanService;
    private final BridgeSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private volatile boolean running;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup blockingGroup;
    private Channel serverChannel;

    public BridgeNettyServer(SkillPortProperties properties, DeviceService deviceService,
                             DownloadTicketService downloadTicketService, InstallTaskService installTaskService,
                             DeviceToolScanService toolScanService,
                             BridgeSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.properties = properties;
        this.deviceService = deviceService;
        this.downloadTicketService = downloadTicketService;
        this.installTaskService = installTaskService;
        this.toolScanService = toolScanService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(properties.netty().workerThreads());
        blockingGroup = new DefaultEventExecutorGroup(Math.max(2, properties.netty().workerThreads()));
        SkillPortNettyHandler handler = new SkillPortNettyHandler(
                deviceService, downloadTicketService, installTaskService, toolScanService, sessionRegistry, objectMapper);
        try {
            serverChannel = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline()
                                    .addLast("httpCodec", new HttpServerCodec())
                                    .addLast("httpAggregator", new HttpObjectAggregator(64 * 1024))
                                    .addLast("chunkedWriter", new ChunkedWriteHandler())
                                    .addLast(blockingGroup, "skillPortRouter", handler);
                        }
                    })
                    .bind(properties.netty().port()).syncUninterruptibly().channel();
            running = true;
            log.info("SkillPort Netty server started on port={}", properties.netty().port());
        } catch (RuntimeException exception) {
            stop();
            throw exception;
        }
    }

    @Override
    public synchronized void stop() {
        if (serverChannel != null) serverChannel.close().syncUninterruptibly();
        if (bossGroup != null) bossGroup.shutdownGracefully().syncUninterruptibly();
        if (workerGroup != null) workerGroup.shutdownGracefully().syncUninterruptibly();
        if (blockingGroup != null) blockingGroup.shutdownGracefully().syncUninterruptibly();
        running = false;
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public int getPhase() { return Integer.MAX_VALUE; }
}
