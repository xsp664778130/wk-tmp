package com.skillport.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeInstanceIdentityTest {
    @TempDir
    Path tempDir;

    @Test
    void keepsTheSameIdentityAcrossRepeatedPairingRuns() {
        Path identityFile = tempDir.resolve("bridge").resolve("instance-id");

        String first = BridgeInstanceIdentity.loadOrCreate(identityFile);
        String second = BridgeInstanceIdentity.loadOrCreate(identityFile);

        assertEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9._-]{8,64}"));
    }
}
