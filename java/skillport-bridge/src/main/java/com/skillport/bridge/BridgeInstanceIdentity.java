package com.skillport.bridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

final class BridgeInstanceIdentity {
    private static final Path ID_FILE = Path.of(System.getProperty("user.home"), ".skillport", "instance-id");

    private BridgeInstanceIdentity() {
    }

    static String loadOrCreate() {
        return loadOrCreate(ID_FILE);
    }

    static String loadOrCreate(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                String existing = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (isValid(existing)) return existing;
            }
            String created = UUID.randomUUID().toString();
            Files.createDirectories(path.getParent());
            Files.writeString(path, created + System.lineSeparator(), StandardCharsets.UTF_8);
            return created;
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存本机实例标识", exception);
        }
    }

    private static boolean isValid(String value) {
        if (value == null || value.length() > 64) return false;
        return value.matches("[A-Za-z0-9._-]{8,64}");
    }
}
