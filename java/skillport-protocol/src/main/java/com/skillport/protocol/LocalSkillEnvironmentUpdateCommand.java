package com.skillport.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record LocalSkillEnvironmentUpdateCommand(String tool, String slug, Map<String, String> values) {
    public LocalSkillEnvironmentUpdateCommand {
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
