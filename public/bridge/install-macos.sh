#!/bin/bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "用法错误：请回到 SkillPort 网站重新复制 macOS 一键安装命令。" >&2
  exit 64
fi

api_base_url="${1%/}"
netty_url="$2"
pairing_code="$3"
install_dir="$HOME/.skillport"
runtime_dir="$install_dir/runtime"
bridge_jar="$install_dir/skillport-bridge.jar"
launcher="$install_dir/run-bridge.sh"
launch_agent="$HOME/Library/LaunchAgents/com.skillport.bridge.plist"
curl_user_agent="Mozilla/5.0 SkillPort-Installer"

step() {
  printf '\n[%s/5] %s\n' "$1" "$2"
}

java_major_version() {
  "$1" -version 2>&1 | awk -F '"' '/version/ { split($2, parts, "."); if (parts[1] == "1") print parts[2]; else print parts[1]; exit }'
}

usable_java() {
  [ -x "$1" ] || command -v "$1" >/dev/null 2>&1 || return 1
  major="$(java_major_version "$1" || true)"
  [ -n "$major" ] && [ "$major" -ge 21 ] 2>/dev/null
}

download_runtime() {
  case "$(uname -m)" in
    arm64|aarch64) runtime_artifact="temurin-jre21-macos-aarch64.tar.gz" ;;
    x86_64|amd64) runtime_artifact="temurin-jre21-macos-x64.tar.gz" ;;
    *) echo "暂不支持此 Mac 架构：$(uname -m)" >&2; exit 65 ;;
  esac

  temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/skillport-java.XXXXXX")"
  trap 'rm -rf "$temp_dir"' EXIT
  archive="$temp_dir/temurin-jre.tar.gz"
  checksum_file="$temp_dir/temurin-jre.sha256.txt"
  download_url="$api_base_url/bridge/runtime/$runtime_artifact"

  echo "正在从 SkillPort 主站下载专用 Java 21（只保存到 ~/.skillport）…"
  curl -A "$curl_user_agent" -fL --retry 3 --connect-timeout 15 "$download_url" -o "$archive"
  curl -A "$curl_user_agent" -fsSL --retry 3 "${download_url}.sha256" -o "$checksum_file"
  expected_hash="$(awk 'NR == 1 { print toupper($1) }' "$checksum_file")"
  actual_hash="$(shasum -a 256 "$archive" | awk '{ print toupper($1) }')"
  [ "$expected_hash" = "$actual_hash" ] || { echo "Java 下载校验失败，已停止安装。" >&2; exit 74; }

  mkdir -p "$runtime_dir"
  tar -xzf "$archive" -C "$runtime_dir" --strip-components=1
  java_bin="$(find "$runtime_dir" -type f -path '*/bin/java' -perm -u+x -print -quit)"
  usable_java "$java_bin" || { echo "Java 21 解压后无法运行。" >&2; exit 70; }
}

printf '\nSkillPort Bridge 快速安装\n不会修改系统 Java，也不需要管理员密码。\n'
mkdir -p "$install_dir" "$install_dir/logs" "$HOME/Library/LaunchAgents"

step 1 "检查运行环境"
java_bin=""
if command -v java >/dev/null 2>&1 && usable_java "$(command -v java)"; then
  java_bin="$(command -v java)"
  echo "已找到可用的 Java $(java_major_version "$java_bin")。"
else
  download_runtime
fi

step 2 "下载并校验 SkillPort Bridge"
curl -A "$curl_user_agent" -fL --retry 3 --connect-timeout 15 "$api_base_url/bridge/skillport-bridge.jar" -o "$bridge_jar"
curl -A "$curl_user_agent" -fsSL --retry 3 "$api_base_url/bridge/skillport-bridge.jar.sha256" -o "$bridge_jar.sha256"
expected_hash="$(awk 'NR == 1 { print toupper($1) }' "$bridge_jar.sha256")"
actual_hash="$(shasum -a 256 "$bridge_jar" | awk '{ print toupper($1) }')"
[ "$expected_hash" = "$actual_hash" ] || { echo "Bridge 下载校验失败，已停止安装。" >&2; exit 74; }

step 3 "绑定当前 SkillPort 账户"
device_name="$(scutil --get ComputerName 2>/dev/null || hostname)"
pair_api_base_url="$api_base_url"
if [ "$api_base_url" = "https://skillport-ai-workspace.mcbbss.chatgpt.site" ]; then
  pair_api_base_url="https://www.jmuyuer.com"
  echo "正在通过 SkillPort 主站安全入口完成配对…"
fi
"$java_bin" -jar "$bridge_jar" pair "$pair_api_base_url" "$netty_url" "$pairing_code" "$device_name"

step 4 "设置登录后自动连接"
printf '%s\n' '#!/bin/bash' "exec \"$java_bin\" -jar \"$bridge_jar\" >>\"$install_dir/logs/bridge.log\" 2>&1" > "$launcher"
chmod 700 "$launcher"
cat > "$launch_agent" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>com.skillport.bridge</string>
  <key>ProgramArguments</key>
  <array><string>/bin/bash</string><string>$launcher</string></array>
  <key>RunAtLoad</key><true/>
  <key>KeepAlive</key><true/>
</dict>
</plist>
EOF
launchctl unload "$launch_agent" >/dev/null 2>&1 || true
launchctl load "$launch_agent"

step 5 "安装完成"
echo "SkillPort Bridge 已连接，并会在你登录 Mac 后自动启动。"
echo "现在可以关闭终端，回到 SkillPort 网页。"
