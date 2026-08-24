#!/bin/bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "用法错误：请回到 SkillPort 网站重新复制 Bridge 更新命令。" >&2
  exit 64
fi

api_base_url="${1%/}"
install_dir="$HOME/.skillport"
bridge_jar="$install_dir/skillport-bridge.jar"
launcher="$install_dir/run-bridge.sh"
launch_agent="$HOME/Library/LaunchAgents/com.skillport.bridge.plist"
curl_user_agent="Mozilla/5.0 SkillPort-Updater"

[ -f "$launcher" ] && [ -f "$launch_agent" ] || {
  echo "尚未找到已安装的 SkillPort Bridge，请先在网站中完成配对。" >&2
  exit 66
}

temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/skillport-update.XXXXXX")"
trap 'rm -rf "$temp_dir"' EXIT
new_jar="$temp_dir/skillport-bridge.jar"
checksum_file="$temp_dir/skillport-bridge.jar.sha256"

echo "[1/3] 下载并校验最新版 Bridge"
curl -A "$curl_user_agent" -fL --retry 3 --connect-timeout 15 "$api_base_url/bridge/skillport-bridge.jar" -o "$new_jar"
curl -A "$curl_user_agent" -fsSL --retry 3 "$api_base_url/bridge/skillport-bridge.jar.sha256" -o "$checksum_file"
expected_hash="$(awk 'NR == 1 { print toupper($1) }' "$checksum_file")"
actual_hash="$(shasum -a 256 "$new_jar" | awk '{ print toupper($1) }')"
[ "$expected_hash" = "$actual_hash" ] || { echo "Bridge 下载校验失败，已停止更新。" >&2; exit 74; }

echo "[2/3] 重启 Bridge"
launchctl unload "$launch_agent" >/dev/null 2>&1 || true
mv "$new_jar" "$bridge_jar"
printf '%s\n' "$expected_hash  skillport-bridge.jar" > "$bridge_jar.sha256"
launchctl load "$launch_agent"

echo "[3/3] 更新完成"
echo "SkillPort Bridge 已更新并重新连接，现在可以使用本机卸载。"
