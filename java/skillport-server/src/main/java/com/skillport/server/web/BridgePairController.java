package com.skillport.server.web;

import com.skillport.server.service.PairingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bridge")
public class BridgePairController {
    private final PairingService pairingService;

    public BridgePairController(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @PostMapping("/pair")
    public PairResponse pair(@Valid @RequestBody PairRequest request) {
        PairingService.PairedDevice device = pairingService.pair(request.code(), request.name(), request.os(), request.arch());
        return new PairResponse(device.deviceId(), device.deviceToken());
    }

    public record PairRequest(@NotBlank String code, @NotBlank String name, @NotBlank String os, @NotBlank String arch) {
    }
    public record PairResponse(String deviceId, String deviceToken) {
    }
}
