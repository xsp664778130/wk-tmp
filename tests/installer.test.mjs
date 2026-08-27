import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { chmod, mkdtemp, readFile, stat, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { promisify } from "node:util";
import { strToU8, zipSync } from "fflate";
import {
  createMacInstaller,
  createMacInstallerArchive,
  createWindowsInstaller,
  installPaths,
  resolveSkillName,
  slugifySkillName,
} from "../app/installer-utils.ts";

const execFileAsync = promisify(execFile);

const manifest = `---
name: release-jdk21-infrastructure-audit
description: Test skill
---

# Test
`;

test("uses the manifest name instead of the generic SKILL.md filename", () => {
  assert.equal(resolveSkillName(strToU8(manifest), "md", "SKILL"), "release-jdk21-infrastructure-audit");
  assert.equal(slugifySkillName("Release JDK21 Infrastructure Audit"), "release-jdk21-infrastructure-audit");
});

test("reads the manifest name from a complete ZIP skill", () => {
  const archive = zipSync({
    "release-audit/SKILL.md": strToU8(manifest),
    "release-audit/scripts/audit.py": strToU8("print('ok')\n"),
  });
  assert.equal(resolveSkillName(archive, "zip", "archive"), "release-jdk21-infrastructure-audit");
});

test("includes every selected tool and creates backups before replacement", () => {
  const paths = installPaths(["codex", "qoder", "opencode", "claude", "cursor"], "release-jdk21-infrastructure-audit");
  assert.deepEqual(paths, [
    ".codex/skills/release-jdk21-infrastructure-audit",
    ".qoder/skills/release-jdk21-infrastructure-audit",
    ".config/opencode/skills/release-jdk21-infrastructure-audit",
    ".claude/skills/release-jdk21-infrastructure-audit",
    ".cursor/skills/release-jdk21-infrastructure-audit",
  ]);
  const mac = createMacInstaller("cGF5bG9hZA==", "zip", paths);
  const windows = createWindowsInstaller("cGF5bG9hZA==", "zip", paths);
  for (const path of paths) assert.match(mac, new RegExp(path.replaceAll(".", "\\.")));
  assert.match(mac, /skillport-backup/);
  assert.match(mac, /base64 -D/);
  assert.match(windows, /skillport-backup/);
  assert.match(windows, /\.qoder\\skills\\release-jdk21-infrastructure-audit/);
  assert.match(windows, /\.config\\opencode\\skills\\release-jdk21-infrastructure-audit/);
  assert.match(windows, /\.claude\\skills\\release-jdk21-infrastructure-audit/);
  assert.match(windows, /\.cursor\\skills\\release-jdk21-infrastructure-audit/);
});

test("the macOS ZIP preserves executable mode and installs a complete skill to every target", async () => {
  const testRoot = await mkdtemp(join(tmpdir(), "skillport-installer-test-"));
  const home = join(testRoot, "home");
  const unpacked = join(testRoot, "unpacked");
  const payload = zipSync({
    "release-audit/SKILL.md": strToU8(manifest),
    "release-audit/scripts/audit.py": strToU8("print('ok')\n"),
  });
  const paths = installPaths(["codex", "qoder"], "release-jdk21-infrastructure-audit");
  const scriptName = "install-release-jdk21-infrastructure-audit.command";
  const script = createMacInstaller(Buffer.from(payload).toString("base64"), "zip", paths);
  const packagePath = join(testRoot, "installer.zip");
  await writeFile(packagePath, createMacInstallerArchive(script, scriptName));
  await execFileAsync("mkdir", ["-p", home, unpacked]);
  await execFileAsync("unzip", ["-q", packagePath, "-d", unpacked]);
  const installerPath = join(unpacked, scriptName);
  assert.equal((await stat(installerPath)).mode & 0o777, 0o755);
  await chmod(installerPath, 0o755);
  await execFileAsync("zsh", [installerPath], { env: { ...process.env, HOME: home } });
  for (const path of paths) {
    assert.match(await readFile(join(home, path, "SKILL.md"), "utf8"), /release-jdk21-infrastructure-audit/);
    assert.equal(await readFile(join(home, path, "scripts/audit.py"), "utf8"), "print('ok')\n");
  }
});
