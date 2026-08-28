package com.skillport.protocol;

public record LocalSkillInfo(
        String tool,
        String slug,
        String name,
        String description,
        String relativePath,
        String originSkillId
) {
}
