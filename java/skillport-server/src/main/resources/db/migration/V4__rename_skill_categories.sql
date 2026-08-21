UPDATE skills
SET category = CASE category
    WHEN '编程开发' THEN '编程技能'
    WHEN '测试工具' THEN '测试技能'
    WHEN '排查工具' THEN '排查技能'
    WHEN '日志报告' THEN '日志技能'
    ELSE category
END
WHERE category IN ('编程开发', '测试工具', '排查工具', '日志报告');

UPDATE public_skills
SET category = CASE category
    WHEN '编程开发' THEN '编程技能'
    WHEN '测试工具' THEN '测试技能'
    WHEN '排查工具' THEN '排查技能'
    WHEN '日志报告' THEN '日志技能'
    ELSE category
END
WHERE category IN ('编程开发', '测试工具', '排查工具', '日志报告');
