package com.skillport.protocol;

import java.time.Instant;
import java.util.List;

public record ToolScanResult(List<String> tools, Instant detectedAt) {
}
