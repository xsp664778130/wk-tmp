import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import test from "node:test";

test("keeps the upload editor visually aligned with every theme", async () => {
  const [styles, client] = await Promise.all([
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(styles, /\.app-shell\[data-theme\] \.upload-modal \.large-upload/);
  assert.match(styles, /\.app-shell\[data-theme\] \.upload-modal \.upload-fields label > span/);
  assert.match(styles, /color-mix\(in srgb, var\(--purple\)/);
  assert.match(styles, /\.upload-modal \.full-primary:disabled/);
  assert.match(client, /version: "1\.0\.24"/);
  assert.match(client, /上传界面主题适配/);
});

test("keeps the dashboard overview focused on useful account metrics", async () => {
  const [styles, client] = await Promise.all([
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(styles, /\.stats-strip \{[^}]*grid-template-columns: repeat\(2, 1fr\)/);
  assert.doesNotMatch(client, /<small>累计加载任务<\/small>/);
  assert.match(client, /version: "1\.0\.25"/);
  assert.match(client, /首页统计精简/);
});

test("keeps every local workspace action row vertically aligned", async () => {
  const [styles, client, desktop] = await Promise.all([
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
    readFile(new URL("../native/skillport_flutter_client/lib/src/workspace.dart", import.meta.url), "utf8"),
  ]);

  assert.match(styles, /\.local-skill-origin-row \{ height: 29px;/);
  assert.match(client, /<div className="local-skill-origin-row">\{skill\.fromMySkills &&/);
  assert.match(desktop, /height: 27,[\s\S]*child: fromMySkills[\s\S]*\? Container/);
});

test("shows and safely edits env.properties across cloud and local Skill cards", async () => {
  const [client, cloudRoute, publicRoute, localRoute, bridge, desktop] = await Promise.all([
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/api/skills/[id]/environment/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/public-skills/[id]/environment/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/devices/[id]/local-skills/environment/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-bridge/src/main/java/com/skillport/bridge/LocalSkillAccess.java", import.meta.url), "utf8"),
    readFile(new URL("../native/skillport_flutter_client/lib/src/workspace.dart", import.meta.url), "utf8"),
  ]);

  assert.match(client, /<span>ENV<\/span>查看配置/);
  assert.match(client, /公有池只读/);
  assert.match(client, /保存后同步公有池文件/);
  assert.match(client, /查看 \/ 编辑 env\.properties/);
  assert.match(cloudRoute, /export async function PATCH/);
  assert.doesNotMatch(publicRoute, /export async function PATCH/);
  assert.match(localRoute, /local-skills\/environment/);
  assert.match(bridge, /ATOMIC_MOVE/);
  assert.match(bridge, /NOFOLLOW_LINKS/);
  assert.match(desktop, /保存 env\.properties/);
});

test("defines the SkillPort workspace and product metadata", async () => {
  const [page, layout, client, packageRoute] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/layout.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/api/skills/[id]/file/route.ts", import.meta.url), "utf8"),
  ]);

  assert.match(layout, /SkillPort — AI Skill 管理工作台/);
  assert.match(page, /getSkillPortUser/);
  assert.match(client, /把好用的 Skill，装进每个 AI/);
  assert.match(client, /注册新账户/);
  assert.match(client, /使用邮箱账户登录或注册/);
  assert.doesNotMatch(client, /企业微信登录 \/ 注册/);
  assert.doesNotMatch(client, /skillport\.wecom-auto-attempted/);
  assert.doesNotMatch(client, /\/api\/auth\/wecom\?mode=auto/);
  assert.match(client, /快速导入/);
  assert.match(client, /本机工具/);
  assert.match(client, /Cursor/);
  assert.match(client, /\.cursor\/skills/);
  assert.match(client, /重新识别/);
  assert.match(client, /scan-tools/);
  assert.match(client, /skillport\.browser-device\./);
  assert.match(client, /请选择当前这台电脑/);
  assert.match(client, /不会自动借用账号中其他电脑的识别结果/);
  assert.match(client, /onDevicePaired/);
  assert.match(client, /automaticToolScanAt/);
  assert.match(client, /60_000/);
  assert.doesNotMatch(client, /devices\.find\(\(device\) => device\.status === "ONLINE"\)/);
  assert.match(client, /未检测到/);
  assert.match(client, /Skill 公有池/);
  assert.match(client, /分享我的 Skill/);
  assert.match(client, /拉取到我的空间/);
  assert.match(client, /个人备注没有公开/);
  assert.match(client, /删除 Skill/);
  assert.match(client, /delete-card-button/);
  assert.match(client, /setActionCandidate\(\{ skill, action: "delete" \}\)/);
  assert.match(client, /从公有池下架/);
  assert.match(client, /从本机卸载/);
  assert.match(client, /打开本地文件夹/);
  assert.match(client, /查看 SKILL\.md/);
  assert.match(client, /不会保存到云端/);
  assert.match(client, /下载 macOS 客户端/);
  assert.match(client, /下载 Windows 客户端/);
  assert.match(client, /版本更新/);
  assert.match(client, /意见信箱/);
  assert.match(client, /fax-stage/);
  assert.match(client, /正在传真你的意见/);
  assert.match(client, /\/api\/feedback/);
  assert.match(client, /skillport\.release-seen/);
  assert.match(client, /skillport\.ui-theme\.v1/);
  assert.match(client, /深夜紫/);
  assert.match(client, /曜石黑/);
  assert.match(client, /海湾蓝/);
  assert.match(client, /晨雾白/);
  assert.match(client, /data-theme=\{theme\}/);
  assert.match(client, /Skill 元数据同步升级/);
  assert.match(client, /以后每次发布都会在这里记录/);
  assert.match(client, /SkillPort-Bridge\.pkg/);
  assert.match(client, /SkillPort-Setup\.exe/);
  assert.match(client, /detectClientPlatform\(navigator\.userAgent\)/);
  assert.match(client, /本机副本会被永久删除/);
  assert.match(client, /\/api\/uninstalls/);
  assert.match(client, /添加 Skill 头像/);
  assert.match(client, /Skill 名称/);
  assert.match(client, /Skill 描述/);
  assert.match(client, /详细说明/);
  assert.match(client, /使用步骤/);
  assert.match(client, /skill-detail-preview/);
  assert.match(client, /form\.append\("detail", metadata\.detail\.trim\(\)\)/);
  assert.match(client, /form\.append\("usageSteps", metadata\.usageSteps\.join\("\\n"\)\)/);
  assert.match(client, /updateSkillDetails/);
  assert.match(client, /form\.append\("name", metadata\.name\.trim\(\)\)/);
  assert.match(client, /form\.append\("description", metadata\.description\.trim\(\)\)/);
  assert.match(client, /分享到公有池时同步使用此名称/);
  assert.match(client, /分享时会同步到公有池/);
  assert.match(client, /updateSkillCategory/);
  assert.match(client, /选择后自动保存并同步公有池/);
  assert.match(client, /void saveCategory\(event\.target\.value as SkillCategory\)/);
  assert.match(client, /setCategory\(previousCategory\)/);
  assert.match(client, /\/api\/skills\/\$\{encodeURIComponent\(skill\.id\)\}/);
  assert.match(client, /image\/png,image\/jpeg,image\/webp,image\/gif/);
  assert.match(client, /编辑 Skill 头像/);
  assert.match(client, /保存头像/);
  assert.match(client, /移除头像/);
  assert.match(client, /只替换文件；名称、描述、详情、步骤、分类、备注和头像均不改变/);
  assert.match(client, /method: "PUT"/);
  assert.match(client, /\/api\/skills\/\$\{encodeURIComponent\(skill\.id\)\}\/file/);
  assert.match(client, /确认替换/);
  assert.match(packageRoute, /export async function PUT/);
  assert.match(packageRoute, /\/api\/v1\/skills\/\$\{encodeURIComponent\(id\)\}\/content/);
  assert.match(client, /version: "1\.0\.27"/);
  assert.match(client, /\/api\/public-skills/);
  for (const category of ["全部技能", "编程技能", "测试技能", "排查技能", "日志技能"]) {
    assert.match(client, new RegExp(`\\["${category}",`));
  }
  for (const legacyCategory of ["编程开发", "测试工具", "排查工具", "日志报告", "数据分析", "创意设计", "效率工具", "商业研究", "自动化"]) {
    assert.doesNotMatch(client, new RegExp(`\\[\\"${legacyCategory}\\",`));
  }
  assert.doesNotMatch(`${page}${layout}${client}`, /codex-preview|Your site is taking shape/);
});

test("keeps dark theme navigation readable and the right rail theme-aware", async () => {
  const css = await readFile(new URL("../app/globals.css", import.meta.url), "utf8");

  assert.match(css, /\.app-shell\[data-theme\] \{ color: var\(--ink\)/);
  assert.match(css, /--sidebar-text: #d6d8e3/);
  assert.match(css, /--sidebar-muted: #8e95a8/);
  assert.match(css, /\.app-shell\[data-theme\] \.tool-row/);
  assert.match(css, /\.app-shell\[data-theme\] \.device-picker/);
  assert.match(css, /background: var\(--rail-surface\)/);
  assert.match(css, /background: linear-gradient\(135deg, var\(--tool-surface\)/);
});

test("provides enterprise WeCom silent authorization and QR login", async () => {
  const [route, service, controller, migration] = await Promise.all([
    readFile(new URL("../app/api/auth/[action]/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/java/com/skillport/server/service/WeComAuthService.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/java/com/skillport/server/web/BrowserController.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V8__add_wecom_identity.sql", import.meta.url), "utf8"),
  ]);

  assert.match(route, /https:\/\/www\.jmuyuer\.com/);
  assert.match(service, /connect\/oauth2\/authorize/);
  assert.match(service, /wwopen\/sso\/qrConnect/);
  assert.match(service, /auth\/getuserinfo/);
  assert.match(service, /cgi-bin\/gettoken/);
  assert.match(controller, /skillport_wecom_state/);
  assert.match(controller, /constantTimeEquals/);
  assert.match(migration, /uk_users_wecom_identity/);
});

test("provides account profiles and one-time email password recovery", async () => {
  const [client, profileRoute, resetRoute, migration, resetStore] = await Promise.all([
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/api/auth/profile/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/auth/password/[action]/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V16__add_password_reset_codes.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/java/com/skillport/server/service/PasswordResetStore.java", import.meta.url), "utf8"),
  ]);

  assert.match(client, /打开个人资料/);
  assert.match(client, /修改密码并退出登录/);
  assert.match(client, /忘记密码？使用邮箱验证码重置/);
  assert.match(client, /验证码将在 10 分钟后失效/);
  assert.match(profileRoute, /api\/v1\/auth\/profile/);
  assert.match(resetRoute, /reset-code/);
  assert.match(migration, /CREATE TABLE password_reset_codes/);
  assert.match(resetStore, /MAX_ATTEMPTS = 5/);
  assert.match(resetStore, /revokeAllByOwnerId/);
  assert.doesNotMatch(resetStore, /code\s*=\s*resetCode\.getCodeHash/);
});

test("ships MySQL accounts, Netty and cross-platform Bridge capabilities", async () => {
  const [hosting, schema, userSchema, publicPoolSchema, avatarSchema, operationSchema, feedbackSchema, publicFeedbackSchema, cursorSchema, detailSchema, instanceSchema, localSkillSchema, client, nettyServer, bridge, scanner, uninstaller, localAccess, localRemoteAccess, toolScanRoute, localSkillRoute, localManifestRoute, localOpenFolderRoute, macInstaller, windowsInstaller, macUpdater, windowsUpdater] = await Promise.all([
    readFile(new URL("../.openai/hosting.json", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V1__init_skillport.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V2__add_local_users.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V3__add_public_skill_pool.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V6__add_skill_avatar.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V7__add_install_task_operation.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V10__add_feedback_mailbox.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V12__publish_feedback_mailbox.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V11__add_cursor_skill_support.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V13__add_skill_details_and_usage_steps.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V14__add_stable_device_identity.sql", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/resources/db/migration/V15__add_device_local_skills.sql", import.meta.url), "utf8"),
    readFile(new URL("../app/skill-workspace.tsx", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/java/com/skillport/server/netty/BridgeNettyServer.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-bridge/src/main/java/com/skillport/bridge/SkillInstaller.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-bridge/src/main/java/com/skillport/bridge/LocalSkillScanner.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-bridge/src/main/java/com/skillport/bridge/SkillUninstaller.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-bridge/src/main/java/com/skillport/bridge/LocalSkillAccess.java", import.meta.url), "utf8"),
    readFile(new URL("../java/skillport-server/src/main/java/com/skillport/server/service/LocalSkillRemoteAccessService.java", import.meta.url), "utf8"),
    readFile(new URL("../app/api/devices/[id]/scan-tools/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/devices/[id]/local-skills/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/devices/[id]/local-skills/manifest/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/api/devices/[id]/local-skills/open-folder/route.ts", import.meta.url), "utf8"),
    readFile(new URL("../public/bridge/install-macos.sh", import.meta.url), "utf8"),
    readFile(new URL("../public/bridge/install-windows.ps1", import.meta.url), "utf8"),
    readFile(new URL("../public/bridge/update-macos.sh", import.meta.url), "utf8"),
    readFile(new URL("../public/bridge/update-windows.ps1", import.meta.url), "utf8"),
  ]);

  assert.match(hosting, /"d1": null/);
  assert.match(hosting, /"r2": null/);
  assert.match(schema, /owner_id/);
  assert.match(userSchema, /CREATE TABLE users/);
  assert.match(userSchema, /CREATE TABLE user_sessions/);
  assert.match(publicPoolSchema, /CREATE TABLE public_skills/);
  assert.match(publicPoolSchema, /source_public_skill_id/);
  assert.match(publicPoolSchema, /UNIQUE KEY uk_skills_owner_source/);
  assert.match(avatarSchema, /avatar_storage_path/);
  assert.match(operationSchema, /operation VARCHAR\(16\)/);
  assert.match(feedbackSchema, /CREATE TABLE feedback_messages/);
  assert.match(feedbackSchema, /idx_feedback_owner_created/);
  assert.match(publicFeedbackSchema, /submitter_display_name/);
  assert.match(publicFeedbackSchema, /idx_feedback_public_created/);
  assert.match(cursorSchema, /codex,qoder,opencode,claude,cursor/);
  assert.match(detailSchema, /detail_text TEXT/);
  assert.match(detailSchema, /usage_steps TEXT/);
  assert.match(instanceSchema, /client_instance_id/);
  assert.match(instanceSchema, /uk_devices_owner_instance/);
  assert.match(localSkillSchema, /CREATE TABLE device_local_skills/);
  assert.match(localSkillSchema, /origin_skill_id/);
  assert.match(nettyServer, /NioServerSocketChannel/);
  assert.match(bridge, /verifySha256/);
  assert.match(scanner, /\.skillport-origin/);
  assert.match(uninstaller, /deleteInstalledSkill/);
  assert.match(toolScanRoute, /scan-tools/);
  assert.match(localSkillRoute, /local-skills\/uninstall/);
  assert.match(localManifestRoute, /local-skills\/manifest/);
  assert.match(localOpenFolderRoute, /local-skills\/open-folder/);
  assert.match(localAccess, /MAX_MANIFEST_BYTES = 512 \* 1024/);
  assert.match(localAccess, /LinkOption\.NOFOLLOW_LINKS/);
  assert.match(localRemoteAccess, /RESPONSE_TIMEOUT/);
  assert.match(client, /个人工作区/);
  assert.match(client, /来自我的 Skill/);
  assert.match(client, /macos/);
  assert.match(client, /windows/);
  assert.match(client, /mktemp -t skillport-installer\.XXXXXX/);
  assert.match(client, /curl -A 'Mozilla\/5\.0 SkillPort-Installer' -fL --retry 3/);
  assert.match(client, /pairApiBaseUrl/);
  assert.match(client, /一键安装命令/);
  assert.match(client, /一键更新 Bridge 命令/);
  assert.match(client, /公开意见墙/);
  assert.match(client, /feedback-pagination/);
  assert.match(client, /setInterval\(\(\) => void pollDevices\(\), 3000\)/);
  assert.match(client, /连接 Bridge（只需一次）/);
  assert.match(client, /临时使用离线安装器/);
  assert.match(client, /首次使用“从本机卸载”时，请更新一次 Bridge/);
  assert.match(client, /powershellUrlExpression/);
  assert.match(client, /Get-Command curl\.exe/);
  assert.match(client, /curl\.exe -fL --retry 3/);
  assert.match(client, /Mozilla\/5\.0 SkillPort-Installer/);
  assert.match(client, /\[IO\.File\]::ReadAllText\(\$temp,\[Text\.Encoding\]::UTF8\)/);
  assert.match(client, /Remove-Item \$temp -Force/);
  assert.doesNotMatch(client, /DownloadData\(\$installer\)/);
  assert.match(client, /不要再套 powershell\.exe -Command/);
  assert.doesNotMatch(client, /powershell\.exe -NoProfile -ExecutionPolicy Bypass -Command/);
  assert.match(client, /prevents rich-text tools from rewriting URLs/);
  assert.match(macInstaller, /bridge\/runtime\/\$runtime_artifact/);
  assert.match(macInstaller, /temurin-jre21-macos-aarch64\.tar\.gz/);
  assert.match(macInstaller, /temurin-jre21-macos-x64\.tar\.gz/);
  assert.doesNotMatch(macInstaller, /api\.adoptium\.net/);
  assert.match(macInstaller, /launchctl load/);
  assert.match(macInstaller, /curl_user_agent="Mozilla\/5\.0 SkillPort-Installer"/);
  assert.match(macInstaller, /curl -A "\$curl_user_agent" -fL --retry 3/);
  assert.match(macInstaller, /pair_api_base_url="https:\/\/www\.jmuyuer\.com"/);
  assert.match(windowsInstaller, /Get-FileHash/);
  assert.match(windowsInstaller, /Invoke-SkillPortDownload/);
  assert.match(windowsInstaller, /bridge\/runtime\/\$RuntimeArtifact/);
  assert.match(windowsInstaller, /temurin-jre21-windows-aarch64\.zip/);
  assert.match(windowsInstaller, /temurin-jre21-windows-x64\.zip/);
  assert.doesNotMatch(windowsInstaller, /api\.adoptium\.net/);
  assert.match(windowsInstaller, /\$Attempt -le 3/);
  assert.doesNotMatch(windowsInstaller, /DownloadUrl\.sha256\.txt/);
  assert.match(windowsInstaller, /System\.Diagnostics\.ProcessStartInfo/);
  assert.match(windowsInstaller, /Get-JavaReleaseMajorVersion/);
  assert.match(windowsInstaller, /Remove-Item -Path \$RuntimeDir -Recurse/);
  assert.match(windowsInstaller, /Directory\.Name -eq "bin"/);
  assert.match(windowsInstaller, /\$PairApiBaseUrl = "https:\/\/www\.jmuyuer\.com"/);
  assert.match(windowsInstaller, /GetFolderPath\("Startup"\)/);
  assert.match(macUpdater, /SkillPort Bridge 已更新并重新连接/);
  assert.match(windowsUpdater, /SkillPort Bridge 已更新并重新连接/);
  await access(new URL("../public/og.png", import.meta.url));
  await assert.rejects(access(new URL("../app\/_sites-preview", import.meta.url)));
});
