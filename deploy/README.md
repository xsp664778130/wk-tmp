# SkillPort Jenkins 自动发布

> 当前阿里云环境使用 K3s。K3s 部署清单位于 `deploy/k3s/`，宿主机 Nginx
> 使用 `deploy/nginx/skillport-k3s.conf`。下面的 systemd/Docker Compose 内容仅作为
> 非 K3s 备选方案保留。

## K3s 生产部署

K3s 版本包含以下工作负载：

- MySQL 8.4，20Gi 持久卷，仅集群内访问。
- SkillPort Spring Boot + Netty，20Gi Skill 文件持久卷。
- Jenkins LTS JDK 21，15Gi 持久卷，只能通过 SSH 隧道访问。
- Headlamp v0.44.0，可视化管理 K3s，只能通过 SSH 隧道访问。
- 集群内 OCI Registry，10Gi 持久卷，仅用于 Jenkins 发布镜像。
- 宿主机 Nginx 保留 80 端口，转发到固定的 ClusterIP。

首次部署由 `deploy/k3s/namespace.yaml`、`mysql.yaml`、`skillport.yaml`、
`jenkins.yaml`、`headlamp.yaml` 完成。生产密钥只创建在 K3s Secret 中，不写入仓库。

Jenkins Pod 使用宿主机只读挂载的 Maven 3.9.11、Node.js 22.21.1 和镜像内 JDK 21，
无需在 Jenkins 页面手工配置全局工具。Jenkinsfile 使用 Jib 在没有 Docker daemon 的情况下
把应用镜像推送到集群内 Registry，再调用 `/usr/local/sbin/skillport-k3s-deploy` 切换
Deployment，失败时恢复上一个镜像。Jenkins ServiceAccount 只有 `skillport` 命名空间内
Deployment 的发布权限，不挂载 K3s 管理 kubeconfig 或 containerd 管理套接字。

访问 Jenkins（SSH 隧道备用入口）：

```bash
ssh -L 8081:127.0.0.1:8081 root@47.116.50.220
```

浏览器打开 `http://127.0.0.1:8081`。

### 通过公网账号密码访问 Jenkins

正式公网入口使用 `https://jenkins.jmuyuer.com`，只复用服务器现有的 80/443 端口，
不要在阿里云防火墙中开放 8080 或 8081。发布公网入口前，必须先通过上面的 SSH 隧道
完成首次初始化，避免未初始化的 Jenkins 被公网用户抢先创建管理员账号。初始化密码只在
服务器本地读取，不要通过聊天或邮件发送：

```bash
sudo k3s kubectl -n skillport exec deployment/jenkins -- \
  cat /var/jenkins_home/secrets/initialAdminPassword
```

首次登录后进入 `Manage Jenkins -> Security`：

- Security Realm 使用 `Jenkins' own user database`。
- 关闭 `Allow users to sign up`，账号只允许管理员创建。
- Authorization 选择 `Logged-in users can do anything` 时，必须关闭匿名读取；多人使用时
  建议安装 Matrix Authorization Strategy 并按用户授予最小权限。
- 保持 CSRF Protection 开启，不允许匿名用户执行构建。

确认管理员账号能够通过 SSH 隧道正常登录后，将 `jenkins.jmuyuer.com` 的 A 记录解析到
`47.116.50.220`，然后在服务器仓库根目录执行：

```bash
sudo install -m 0644 deploy/nginx/jenkins-k3s.conf /etc/nginx/conf.d/jenkins-k3s.conf
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d jenkins.jmuyuer.com
```

证书签发成功后，浏览器打开 `https://jenkins.jmuyuer.com`。再进入
`Manage Jenkins -> System -> Jenkins Location`，将 Jenkins URL 设置为
`https://jenkins.jmuyuer.com/`。SSH 隧道入口继续保留，用于域名、证书或反向代理故障时维护。

访问 K3s 可视化管理 Headlamp：

```bash
ssh -L 8082:127.0.0.1:8082 root@47.116.50.220
```

浏览器打开 `http://127.0.0.1:8082`。登录使用集群生成、可随时撤销的长期只读 Token。
在 macOS 上可以直接复制到剪贴板：

```bash
ssh root@47.116.50.220 \
  'k3s kubectl get secret headlamp-viewer-token -n kube-system -o jsonpath="{.data.token}"' \
  | base64 -D | pbcopy
```

`headlamp-viewer` 仅绑定 Kubernetes 内置的 `view` 角色，可查看资源和日志，不能在网页中
删除或修改生产工作负载。需要撤销 Token 时删除 `headlamp-viewer-token` Secret，再重新应用
`deploy/k3s/headlamp.yaml` 即可生成新 Token。Headlamp 浏览器会话有效期配置为 30 天。

没有 SSH 终端时，可以应用 `deploy/k3s/headlamp-tunnel.yaml` 创建临时 HTTPS 公网入口。
该入口使用 Cloudflare Quick Tunnel，不开放服务器入站端口；公网地址会在 Pod 重建后变化，
适合临时访问，不作为正式生产域名。

React 页面由 `npm run build:static` 输出到 `dist/static`，Maven 随后把它复制进
Spring Boot 的 `classpath:/static`。因此 `skillport-server` 单一镜像同时提供首页、
同域 `/api/` 浏览器接口和 `/api/v1/` 网关接口，不需要单独部署 Node Web 服务。
临时公网入口使用 Cloudflare Quick Tunnel 提供 HTTPS；绑定已备案域名后应改用
固定域名的 Named Tunnel 或标准 HTTPS Ingress。

Named Tunnel 使用 `deploy/k3s/skillport-named-tunnel.yaml`，Token 仅保存在
`skillport-cloudflare-tunnel` Secret 的 `token` 字段中。Cloudflare 的公开路由应配置为：

- `www.jmuyuer.com` → `http://skillport-server.skillport.svc.cluster.local:8080`
- `bridge.jmuyuer.com` → `http://skillport-server.skillport.svc.cluster.local:9091`

Named Tunnel 验证正常后再将 `skillport-public-tunnel` 缩容为 0，以保留快速回滚能力。

这套配置面向一台 Ubuntu 阿里云轻量应用服务器：Jenkins、MySQL、SkillPort API 和 Netty 在同一台机器运行，公网只开放 SSH、HTTP、HTTPS。

```text
Git main 分支
  -> Jenkins（每 2 分钟检查代码）
  -> Web 测试 + 静态构建
  -> Java 测试/打包（内含 Web 静态资源）
  -> Jib 构建单一生产镜像
  -> K3s Deployment 滚动更新
  -> /actuator/health 健康检查
  -> 失败自动恢复上一镜像
```

Jenkins 负责 React 页面、Java 服务端和 Bridge JAR 的全自动发布。浏览器账户登录由
Java 服务使用 HttpOnly Cookie 处理；网关密钥只用于受保护的 `/api/v1/` 服务端接口，
不会写入前端资源或浏览器存储。

## 一、服务器准备

建议配置至少 4GB 内存、50GB 磁盘。如果服务器只有 2GB 内存，建议把 Jenkins 放在另一台机器，生产服务器只接收构建产物。

在阿里云轻量应用服务器防火墙中：

- `22`：只允许你的固定公网 IP。
- `80`、`443`：允许公网访问。
- 不开放 `3306`、`8080`、`8081`、`9091`。

将 `api.your-domain.com` 解析到服务器公网 IP。中国内地服务器上的域名需要先完成备案。

安装 Docker。可以使用阿里云 Docker 应用镜像，也可以按照 Docker 官方方式安装 Docker Engine 和 Compose 插件。

## 二、安装 Jenkins LTS

下面以 Ubuntu 24.04 为例；如果服务器是 Alibaba Cloud Linux、CentOS 或 Debian，需要换用对应的软件包命令。Jenkins 当前要求 Java 21 或更高版本，应先安装 Java：

```bash
sudo apt update
sudo apt install -y fontconfig openjdk-21-jre
sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key
echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list >/dev/null
sudo apt update
sudo apt install -y jenkins
```

SkillPort 使用 `8080`，因此让 Jenkins 仅监听本机 `8081`：

```bash
sudo systemctl edit jenkins
```

写入：

```ini
[Service]
Environment="JENKINS_PORT=8081"
Environment="JENKINS_OPTS=--httpListenAddress=127.0.0.1"
```

然后启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable jenkins
sudo systemctl restart jenkins
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

本机通过 SSH 隧道访问 Jenkins，无需在阿里云开放 `8081`：

```bash
ssh -L 8081:127.0.0.1:8081 root@服务器公网IP
```

浏览器打开 `http://127.0.0.1:8081`。

## 三、初始化 SkillPort 运行环境

先把仓库克隆到服务器临时目录，然后在仓库根目录执行：

```bash
sudo bash deploy/scripts/bootstrap-ubuntu.sh api.your-domain.com
```

该脚本会安装 JDK 21、Maven、Nginx，创建低权限 `skillport` 用户，并安装 systemd、Nginx、发布和回滚脚本。它不会使用示例密码启动数据库。

编辑两个配置文件：

```bash
sudoedit /etc/skillport/mysql.env
sudoedit /etc/skillport/skillport.env
```

要求：

- 两个文件中的 `MYSQL_PASSWORD` 必须相同。
- `MYSQL_ROOT_PASSWORD` 使用另一条随机密码。
- `SKILLPORT_GATEWAY_KEY` 至少 32 个随机字符。
- 将配置中的 `api.example.com` 全部改成真实 API 域名。
- 不要把这些文件提交到 Git，也不要把密码写进 Jenkinsfile。

可用下面的命令分别生成随机值：

```bash
openssl rand -base64 36
```

启动 MySQL：

```bash
sudo docker compose -f /opt/skillport/docker-compose.prod.yml up -d
```

## 四、配置 HTTPS

Nginx 初始配置允许 HTTP，便于申请证书。安装 Certbot 并让它自动更新 Nginx：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.your-domain.com
sudo nginx -t
sudo systemctl reload nginx
```

HTTPS 配置完成后，同一个域名会提供：

- `/api/`：Spring Boot API。
- `/ws/bridge`：Netty WebSocket，经 Nginx 升级为 WSS。
- `/downloads/`：支持 Range 的 Skill 下载。
- `/bridge/skillport-bridge.jar`：当前 Bridge 安装包。

## 五、配置 Jenkins

在“Manage Jenkins → Plugins”安装：

- Pipeline
- Git
- NodeJS

在“Manage Jenkins → Tools”添加以下名称，必须与 [Jenkinsfile](../Jenkinsfile) 一致：

- JDK：`jdk21`
- Maven：`maven3`
- NodeJS：`node22`，版本选择满足项目要求的 Node.js 22。

JDK 可以指向 `/usr/lib/jvm/java-21-openjdk-amd64`（ARM 服务器按实际目录填写），Maven 可以启用自动安装；NodeJS 使用插件的自动安装能力。

创建“Pipeline”任务：

1. Definition 选择 `Pipeline script from SCM`。
2. SCM 选择 Git，填仓库地址；私有仓库选择 Jenkins Credential。
3. Branch Specifier 填 `*/main`。
4. Script Path 填 `Jenkinsfile`。
5. 保存并手动运行第一次构建。

Jenkinsfile 每两分钟检查一次 Git；`main` 分支有新提交时会自动测试、打包并部署。使用轮询是为了让 Jenkins 保持仅监听本机，避免公开管理后台。其他分支只构建和测试，不部署。

## 六、访问 Web

Spring Boot 根路径 `/` 直接提供 SkillPort 页面，页面使用同域 `/api/` 完成注册、登录、
上传、备注、设备配对和安装任务。正式域名应以 HTTPS 指向该服务；不要使用裸 HTTP
提交数据库账户密码。`/api/v1/` 保留给可信服务端或网关，并继续要求
`X-SkillPort-Gateway-Key`。

## 运维命令

```bash
# 服务状态和日志
sudo systemctl status skillport
sudo journalctl -u skillport -f

# 查看版本
ls -la /opt/skillport/releases /opt/skillport/current

# 手动回滚
sudo /usr/local/sbin/skillport-rollback <release-id>

# 健康检查
curl --fail https://api.your-domain.com/actuator/health
```

正式运行前应增加 MySQL 定时备份，并将备份同步到另一台机器或 OSS；单机磁盘不能视为备份。
后续数据库迁移应保持向后兼容，否则即使 JAR 自动回滚，旧版本也可能无法兼容已经升级的表结构。
