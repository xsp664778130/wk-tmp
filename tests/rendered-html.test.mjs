import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import test from "node:test";

test("defines the SkillPort workspace and product metadata", async () => {
  const [page, layout, client] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(layout, /SkillPort — AI Skill 管理工作台/);
  assert.match(page, /getSkillPortUser/);
  assert.match(client, /把好用的 Skill，装进每个 AI/);
  assert.match(client, /注册新账户/);
  assert.match(client, /快速导入/);
  assert.match(client, /本机工具/);
  assert.doesNotMatch(`${page}${layout}${client}`, /codex-preview|Your site is taking shape/);
});

test("ships MySQL accounts, Netty and cross-platform Bridge capabilities", async () => {
  const [hosting, schema, userSchema, client, nettyServer, bridge] = await Promise.all([
    readFile(new URL("../.openai/hosting.json", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V1__init_skillport.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V2__add_local_users.sql", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/java/com/skillport/server/netty/BridgeNettyServer.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-bridge/src/main/java/com/skillport/bridge/SkillInstaller.java", import.meta.url), "utf8"),
  ]);

  assert.match(hosting, /"d1": null/);
  assert.match(hosting, /"r2": null/);
  assert.match(schema, /owner_id/);
  assert.match(userSchema, /CREATE TABLE users/);
  assert.match(userSchema, /CREATE TABLE user_sessions/);
  assert.match(nettyServer, /NioServerSocketChannel/);
  assert.match(bridge, /verifySha256/);
  assert.match(client, /macos/);
  assert.match(client, /windows/);
  await access(new URL("../public/og.png", import.meta.url));
  await assert.rejects(access(new URL("../app\/_sites-preview", import.meta.url)));
});
