package com.skillport.protocol;

public record LocalSkillActionResult(
        String tool,
        String slug,
        String action,
        boolean success,
        String message,
        String content
) {
    public static LocalSkillActionResult opened(String tool, String slug) {
        return new LocalSkillActionResult(tool, slug, "OPEN_FOLDER", true, "本地文件夹已打开", null);
    }

    public static LocalSkillActionResult manifest(String tool, String slug, String content) {
        return new LocalSkillActionResult(tool, slug, "READ_MANIFEST", true, "SKILL.md 读取成功", content);
    }

    public static LocalSkillActionResult failed(String tool, String slug, String action, String message) {
        return new LocalSkillActionResult(tool, slug, action, false, message, null);
    }
}
