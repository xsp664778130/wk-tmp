UPDATE public_skills
SET tool_compatibility = 'codex,qoder,opencode,claude,cursor'
WHERE tool_compatibility <> 'codex,qoder,opencode,claude,cursor';
