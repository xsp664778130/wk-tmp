import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../", import.meta.url);

test("defines the SkillPort workspace and product metadata", async () => {
  const [page, layout, client] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(layout, /SkillPort — AI Skill 管理工作台/);
  assert.match(page, /getChatGPTUser/);
  assert.match(client, /把好用的 Skill，装进每个 AI/);
  assert.match(client, /快速导入/);
  assert.match(client, /本机工具/);
  assert.doesNotMatch(`${page}${layout}${client}`, /codex-preview|Your site is taking shape/);
});

test("ships persistent personal-data capabilities", async () => {
  const [hosting, schema, client] = await Promise.all([
    readFile(new URL("../.openai/hosting.json", import.meta.url), "utf8"),
    readFile(new URL("../db/schema.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(hosting, /"d1": "DB"/);
  assert.match(hosting, /"r2": "SKILL_FILES"/);
  assert.match(schema, /ownerId/);
  assert.match(client, /macos/);
  assert.match(client, /windows/);
  await access(new URL("../public/og.png", import.meta.url));
  await assert.rejects(access(new URL("../app\/_sites-preview", import.meta.url)));
});
