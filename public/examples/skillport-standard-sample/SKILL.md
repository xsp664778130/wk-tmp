---
name: skillport-standard-sample
description: Validate a SkillPort upload with a minimal standards-compliant Skill package. Use when testing Skill upload, download, or installation.
---

# SkillPort Standard Sample

Use this Skill to verify that a standards-compliant package survives upload and installation.

## Workflow

1. Confirm this `SKILL.md` is present at the Skill root.
2. Run `scripts/check.sh` when a shell is available.
3. Report whether the marker `SKILLPORT_SAMPLE_OK` is printed.

Do not use this sample as a general system diagnostic.
