package com.skillport.server.service;

import com.skillport.server.domain.DeviceEntity;
import com.skillport.server.domain.PairingCodeEntity;
import com.skillport.server.repository.DeviceRepository;
import com.skillport.server.repository.PairingCodeRepository;
import com.skillport.server.security.TokenService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PairingServiceTest {
    @Test
    void reusesTheDeviceRecordForTheSameStableBridgeInstance() {
        PairingCodeRepository pairingCodes = mock(PairingCodeRepository.class);
        DeviceRepository devices = mock(DeviceRepository.class);
        TokenService tokens = new TokenService();
        PairingService service = new PairingService(pairingCodes, devices, tokens);
        Instant now = Instant.now();
        PairingCodeEntity code = new PairingCodeEntity(
                tokens.sha256("ABCD12"), "owner-1", now.plus(5, ChronoUnit.MINUTES), now);
        DeviceEntity existing = new DeviceEntity(
                "device-existing", "owner-1", "instance-1111", "Mac mini", "macos", "aarch64", "old-hash", now);
        when(pairingCodes.findById(tokens.sha256("ABCD12"))).thenReturn(Optional.of(code));
        when(devices.findByOwnerIdAndClientInstanceId("owner-1", "instance-1111"))
                .thenReturn(Optional.of(existing));
        when(devices.save(any(DeviceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PairingService.PairedDevice paired = service.pair(
                "ABCD-12", "Mac mini", "macos", "aarch64", "instance-1111");

        assertEquals("device-existing", paired.deviceId());
        assertEquals("instance-1111", existing.getClientInstanceId());
        verify(devices).save(existing);
    }
}
