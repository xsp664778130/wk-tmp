package com.skillport.server.netty;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BridgeSessionRegistry {
    private final Map<String, Channel> channelsByDevice = new ConcurrentHashMap<>();

    public void register(String deviceId, Channel channel) {
        Channel previous = channelsByDevice.put(deviceId, channel);
        if (previous != null && previous != channel) previous.close();
    }

    public void unregister(String deviceId, Channel channel) {
        channelsByDevice.remove(deviceId, channel);
    }

    public boolean isOnline(String deviceId) {
        Channel channel = channelsByDevice.get(deviceId);
        return channel != null && channel.isActive();
    }

    public boolean send(String deviceId, String json) {
        Channel channel = channelsByDevice.get(deviceId);
        if (channel == null || !channel.isActive()) return false;
        channel.writeAndFlush(new TextWebSocketFrame(json));
        return true;
    }
}
