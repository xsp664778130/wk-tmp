package com.skillport.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class SkillPortBridgeApplication {
    private SkillPortBridgeApplication() {
    }

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        if (args.length > 0 && args[0].equalsIgnoreCase("pair")) {
            pair(args, objectMapper);
            return;
        }
        BridgeConfig config = BridgeConfig.load();
        new BridgeWebSocketClient(config, objectMapper).runForever();
    }

    private static void pair(String[] args, ObjectMapper objectMapper) {
        if (args.length < 4) {
            System.err.println("用法: java -jar skillport-bridge.jar pair <API_URL> <NETTY_URL> <PAIR_CODE> [DEVICE_NAME]");
            System.exit(2);
        }
        String name = args.length >= 5 ? args[4] : System.getProperty("user.name", "My Computer") + "'s computer";
        String clientInstanceId = BridgeInstanceIdentity.loadOrCreate();
        PairingClient.PairResult result = new PairingClient(objectMapper)
                .pair(args[1], args[3], name, clientInstanceId);
        new BridgeConfig(args[1], args[2], result.deviceId(), result.deviceToken()).save();
        System.out.println("配对成功，设备ID=" + result.deviceId());
    }
}
