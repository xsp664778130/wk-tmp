# SkillPort Flutter Desktop

真正独立的 SkillPort 桌面客户端，支持 Windows 与 macOS。应用内完成登录、私人 Skill 管理、公有池浏览与拉取、拖动上传、分类、备注、分享、本机安装和无备份卸载，不会启动浏览器，也不依赖常驻 JAR。客户端使用服务端普通用户会话接口，不包含或泄露网关密钥。

## 本地开发

```bash
flutter pub get
flutter analyze
flutter test
flutter run -d macos      # 必须在安装完整 Xcode 的 macOS 上运行
flutter run -d windows    # 必须在安装 Visual Studio C++ 工具链的 Windows 上运行
```

## 发布构建

```bash
flutter build macos --release
flutter build windows --release
```

Flutter 官方桌面构建不能跨操作系统：Windows 产物必须在 Windows 构建机生成，macOS 产物必须在安装完整 Xcode 的 Mac 上生成。仓库中的 GitHub Actions 工作流会分别使用对应系统构建并上传产物。

安装完成后，Windows 与 macOS 都会自动在当前用户桌面创建 `SkillPort` 快捷方式。Windows 固定安装到 `%LOCALAPPDATA%\Programs\SkillPort`，macOS 固定安装到 `/Applications/SkillPort.app`；macOS 安装完成后还会自动启动一次客户端。客户端、安装程序和桌面快捷方式统一使用 SKILL 品牌图标。

macOS Release 构建在打包前会移除临时签名中错误携带的受限调试权限并重新签名；工作流会拒绝发布仍包含 `get-task-allow` 的 App。当前最低支持 macOS 12。

## 本机安装安全

- 所有下载内容必须通过服务端 SHA-256 校验。
- ZIP 拒绝绝对路径、`..`、符号链接和超过 100MB 的解压内容。
- 安装使用同目录临时目录完成校验后原子替换。
- 卸载只删除所选 AI 工具中的 Skill 目录，不生成备份，也不删除云端内容。
- Windows 登录令牌保存在系统安全存储中。当前公网 macOS 客户端尚未使用 Apple Developer ID 正式签名，为避免 Keychain 返回 `-34018`，令牌保存在当前用户的 `~/Library/Application Support/SkillPort/session.token`，目录权限为 `700`、文件权限为 `600`；配置正式签名后再迁回 Keychain。
