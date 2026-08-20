# SkillPort

SkillPort 是一个按用户隔离的 AI Skill 管理与分发平台。网页负责搜索、上传、备注和发起安装；Java 云端负责 MySQL 数据、设备配对和权限；Netty 负责 Bridge 长连接、断点下载和安装进度；Bridge 在 macOS/Windows 本机完成校验与安装。

## 工程结构

- `app/`：SkillPort Web 管理端和 Java API 网关
- `java/skillport-server/`：Spring Boot + MySQL + Netty 云端服务
- `java/skillport-bridge/`：macOS/Windows 本机 Bridge
- `java/skillport-protocol/`：服务端和客户端共享的通信协议

## Web 管理端

```bash
npm install
cp .env.example .env.local
npm run dev
```

Web 端只在服务端读取 ChatGPT 登录身份，并用私有网关密钥调用 Java 服务。浏览器不会接触网关密钥。

## Java 平台

完整的 MySQL、服务端和 Bridge 启动说明见 [`java/README.md`](java/README.md)。

阿里云轻量应用服务器上的 Jenkins 自动构建、健康检查、失败回滚和 HTTPS 配置见
[`deploy/README.md`](deploy/README.md)。

当前 K3s 生产清单位于 [`deploy/k3s/`](deploy/k3s/)，包含 MySQL、SkillPort 和 Jenkins。

## 验证

```bash
npm test
cd java && mvn test
```
