package com.skillport.server.storage;

import java.nio.file.Path;

public record StoredSkillFile(Path path, long sizeBytes, String sha256) {
}
