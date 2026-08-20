package com.skillport.server.security;

import java.util.HexFormat;

final class HexFormatSupport {
    private HexFormatSupport() {
    }

    static String toHex(byte[] value) {
        return HexFormat.of().formatHex(value);
    }
}
