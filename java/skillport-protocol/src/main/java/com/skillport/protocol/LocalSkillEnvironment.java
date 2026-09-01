package com.skillport.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record LocalSkillEnvironment(boolean exists, String path, Map<String, String> values) {
    public LocalSkillEnvironment {
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static LocalSkillEnvironment missing() {
        return new LocalSkillEnvironment(false, "env.properties", Map.of());
    }
}
