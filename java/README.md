# SkillPort Java Platform

## 运行架构

```text
Web 管理端 --HTTPS--> Spring Boot API :8080 --JPA/Flyway--> MySQL 8
                              |
                              +--> Netty :9091 <--WebSocket--> Desktop Bridge
                                      |
                                      +--HTTP Range 下载--> Desktop Bridge
```

Netty 的数据库鉴权和文件解析运行在独立执行器中，不占用 I/O EventLoop。下载支持 `Range` 断点续传，客户端完成 SHA-256 校验后才会解压；ZIP 解压会阻止目录穿越。

## 要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+
- Docker 可选，只用于快速启动 MySQL

## 1. 启动 MySQL

有 Docker 时：

```bash
docker compose up -d mysql
```

也可以使用已有 MySQL，创建名为 `skillport` 的数据库并通过环境变量提供连接信息。Flyway 会自动建立表和索引。

## 2. 构建并启动服务端

```bash
mvn clean package

export MYSQL_URL='jdbc:mysql://127.0.0.1:3306/skillport?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false'
export MYSQL_USER='skillport'
export MYSQL_PASSWORD='skillport'
export SKILLPORT_GATEWAY_KEY='至少32位随机密钥'
export SKILLPORT_API_PUBLIC_URL='https://api.example.com'
export SKILLPORT_NETTY_PUBLIC_URL='http://127.0.0.1:9091'

java -jar skillport-server/target/skillport-server-1.0.0-SNAPSHOT.jar
```

无 Docker daemon 构建 K3s 可导入的镜像包：

```bash
mvn -DskipTests install
mvn -f skillport-server/pom.xml -DskipTests \
  -Djib.to.image=skillport/server:local \
  com.google.cloud.tools:jib-maven-plugin:3.5.2:buildTar
```

产物为 `skillport-server/target/skillport-server-image.tar`。

服务端端口：

- `8080`：Spring Boot REST API
- `9091`：Netty WebSocket 和断点下载

生产环境应在负载均衡器终止 TLS，并分别转发：

- `/api/` → `8080`
- `/ws/bridge`、`/downloads/` → `9091`

Netty WebSocket 需要启用 Upgrade 转发，下载路由需要保留 `Range`、`Content-Range` 和 `Accept-Ranges`。

## 3. 配置 Web

Web/Sites 运行环境需要：

```text
SKILLPORT_BACKEND_URL=https://api.example.com
SKILLPORT_GATEWAY_KEY=与Java服务相同的密钥
```

建议让 Web 通过私有网络访问 `8080`，不要把网关密钥交给浏览器。

## 4. 配对 Bridge

先在 Web 端调用“生成配对码”，然后在客户端机器运行：

```bash
java -jar skillport-bridge/target/skillport-bridge-1.0.0-SNAPSHOT.jar \
  pair https://api.example.com wss://bridge.example.com ABCD-EFGH "My Mac"
```

配对成功后直接启动：

```bash
java -jar skillport-bridge/target/skillport-bridge-1.0.0-SNAPSHOT.jar
```

Bridge 配置保存在用户目录的 `.skillport/bridge.properties`，设备令牌只保存明文客户端副本；MySQL 中仅保存 SHA-256 哈希。

## 5. 本机目录

Bridge 根据当前用户主目录自动兼容 macOS 和 Windows：

- Codex：`.codex/skills/<skill>`
- Qoder：`.qoder/skills/<skill>`
- OpenAI：`.openai/skills/<skill>`

## 安全与运维

- 配对码 10 分钟过期且只能使用一次
- 下载票据 10 分钟过期
- Skill、设备、任务查询均强制匹配 `owner_id`
- 单个上传文件限制 25MB
- 服务端只记录 ID、状态和错误摘要，不记录实体、文件内容或设备令牌
- 生产环境应把 `SKILLPORT_STORAGE_ROOT` 挂载到持久卷；多实例部署时可将 `FileStorageService` 替换为 S3/R2/OSS 实现
