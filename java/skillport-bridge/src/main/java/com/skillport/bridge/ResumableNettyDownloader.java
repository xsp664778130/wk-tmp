package com.skillport.bridge;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ResumableNettyDownloader {
    public Path download(String url, Path target, long expectedSize, ProgressListener listener) {
        EventLoopGroup networkGroup = new NioEventLoopGroup(1);
        DefaultEventExecutorGroup fileGroup = new DefaultEventExecutorGroup(1);
        try {
            Files.createDirectories(target.getParent());
            long existingSize = Files.exists(target) ? Files.size(target) : 0L;
            if (existingSize > expectedSize) {
                Files.delete(target);
                existingSize = 0L;
            }
            CompletableFuture<Path> completed = new CompletableFuture<>();
            connectAndDownload(networkGroup, fileGroup, URI.create(url), target, existingSize, expectedSize, listener, completed);
            return completed.get(30, TimeUnit.MINUTES);
        } catch (Exception exception) {
            throw new IllegalStateException("Skill 下载失败", exception);
        } finally {
            fileGroup.shutdownGracefully().syncUninterruptibly();
            networkGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    private void connectAndDownload(EventLoopGroup networkGroup, DefaultEventExecutorGroup fileGroup, URI uri,
                                    Path target, long offset, long expectedSize, ProgressListener listener,
                                    CompletableFuture<Path> completed) throws Exception {
        boolean ssl = uri.getScheme().equalsIgnoreCase("https");
        int port = uri.getPort() > 0 ? uri.getPort() : ssl ? 443 : 80;
        SslContext sslContext = ssl ? SslContextBuilder.forClient().build() : null;
        DownloadHandler handler = new DownloadHandler(target, offset, expectedSize, listener, completed);
        Channel channel = new Bootstrap()
                .group(networkGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        if (sslContext != null) channel.pipeline().addLast(sslContext.newHandler(channel.alloc(), uri.getHost(), port));
                        channel.pipeline().addLast(new HttpClientCodec());
                        channel.pipeline().addLast(fileGroup, handler);
                    }
                })
                .connect(uri.getHost(), port).sync().channel();

        String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        if (offset > 0) request.headers().set(HttpHeaderNames.RANGE, "bytes=" + offset + "-");
        channel.writeAndFlush(request);
    }

    private static final class DownloadHandler extends SimpleChannelInboundHandler<HttpObject> {
        private final Path target;
        private final long initialOffset;
        private final long expectedSize;
        private final ProgressListener listener;
        private final CompletableFuture<Path> completed;
        private RandomAccessFile file;
        private long written;

        private DownloadHandler(Path target, long initialOffset, long expectedSize, ProgressListener listener,
                                CompletableFuture<Path> completed) {
            this.target = target;
            this.initialOffset = initialOffset;
            this.expectedSize = expectedSize;
            this.listener = listener;
            this.completed = completed;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, HttpObject message) throws Exception {
            if (message instanceof HttpResponse response) {
                int status = response.status().code();
                if (status != 200 && status != 206) throw new IOException("下载响应状态=" + status);
                file = new RandomAccessFile(target.toFile(), "rw");
                if (status == 200) file.setLength(0);
                written = status == 206 ? initialOffset : 0L;
                file.seek(written);
            }
            if (message instanceof HttpContent content) {
                ByteBuf buffer = content.content();
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                file.write(bytes);
                written += bytes.length;
                int progress = expectedSize <= 0 ? 0 : (int) Math.min(90, written * 90 / expectedSize);
                listener.onProgress(progress);
                if (message instanceof LastHttpContent) {
                    closeFile();
                    if (expectedSize > 0 && written != expectedSize) {
                        throw new IOException("文件长度不一致 expected=" + expectedSize + " actual=" + written);
                    }
                    completed.complete(target);
                    context.close();
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            closeFile();
            completed.completeExceptionally(cause);
            context.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) {
            closeFile();
            if (!completed.isDone()) completed.completeExceptionally(new IOException("下载连接提前关闭"));
        }

        private void closeFile() {
            if (file == null) return;
            try { file.close(); } catch (IOException ignored) { }
            file = null;
        }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int progress);
    }
}
