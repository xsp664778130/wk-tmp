package com.skillport.protocol;

public record LocalSkillActionResult(
        String tool,
        String slug,
        String action,
        boolean success,
        String message,
        String content,
        LocalSkillEnvironment environment
) {
    public static LocalSkillActionResult opened(String tool, String slug) {
        return new LocalSkillActionResult(tool, slug, "OPEN_FOLDER", true, "本地文件夹已打开", null, null);
    }

    public static LocalSkillActionResult manifest(String tool, String slug, String content) {
        return new LocalSkillActionResult(tool, slug, "READ_MANIFEST", true, "SKILL.md 读取成功", content, null);
    }

    public static LocalSkillActionResult environment(String tool, String slug, String action,
                                                     LocalSkillEnvironment environment) {
        String message = environment.exists() ? "env.properties 读取成功" : "该 Skill 未包含 env.properties";
        if ("UPDATE_ENVIRONMENT".equals(action)) message = "env.properties 已保存";
        return new LocalSkillActionResult(tool, slug, action, true, message, null, environment);
    }

    public static LocalSkillActionResult failed(String tool, String slug, String action, String message) {
        return new LocalSkillActionResult(tool, slug, action, false, message, null, null);
    }
}
