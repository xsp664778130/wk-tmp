import { strFromU8, strToU8, unzipSync, zipSync } from "fflate";

export const installerTargetRoots = {
  codex: ".codex/skills",
  qoder: ".qoder/skills",
  opencode: ".config/opencode/skills",
  claude: ".claude/skills",
  cursor: ".cursor/skills",
} as const;

export type InstallerTarget = keyof typeof installerTargetRoots;

function manifestName(markdown: string) {
  const frontmatter = markdown.match(/^---\s*\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/)?.[1];
  if (!frontmatter) return "";
  const rawName = frontmatter.match(/^\s*name\s*:\s*(.+?)\s*$/im)?.[1]?.trim() ?? "";
  if (!rawName) return "";
  const unquoted = rawName.match(/^(?:"([\s\S]*)"|'([\s\S]*)')$/);
  return (unquoted?.[1] ?? unquoted?.[2] ?? rawName).trim().slice(0, 160);
}

export function resolveSkillName(payload: Uint8Array, extension: string, fallbackName: string) {
  try {
    if (isArchiveExtension(extension)) {
      const files = unzipSync(payload, {
        filter: (file) => /(^|\/)SKILL\.md$/i.test(file.name) && !file.name.startsWith("__MACOSX/"),
      });
      const manifests = Object.entries(files)
        .filter(([path]) => /(^|\/)SKILL\.md$/i.test(path) && !path.startsWith("__MACOSX/"))
        .sort(([left], [right]) => left.split("/").length - right.split("/").length);
      if (manifests.length) return manifestName(strFromU8(manifests[0][1])) || fallbackName;
    } else {
      return manifestName(strFromU8(payload)) || fallbackName;
    }
  } catch {
    // The installer still supports the original filename/display name when a malformed archive is uploaded.
  }
  return fallbackName;
}

export function slugifySkillName(value: string) {
  return value.toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fff]+/g, "-")
    .replace(/^-+|-+$/g, "") || "skillport-skill";
}

export function isArchiveExtension(extension: string) {
  return extension.toLowerCase() === "zip" || extension.toLowerCase() === "skill";
}

export function installPaths(targets: string[], slug: string) {
  return targets
    .filter((target): target is InstallerTarget => target in installerTargetRoots)
    .map((target) => `${installerTargetRoots[target]}/${slug}`);
}

export function createMacInstaller(base64: string, extension: string, paths: string[]) {
  const destinations = paths.map((path) => `"$HOME/${path}"`).join(" ");
  const archive = isArchiveExtension(extension);
  return `#!/bin/zsh
set -euo pipefail
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
PAYLOAD="$TEMP_DIR/skill.${archive ? "zip" : "md"}"
printf '%s' '${base64}' | base64 -D > "$PAYLOAD"
SOURCE_ROOT=""
if ${archive ? "true" : "false"}; then
  ARCHIVE_DIR="$TEMP_DIR/archive"
  mkdir -p "$ARCHIVE_DIR"
  while IFS= read -r ENTRY; do
    case "$ENTRY" in
      /*|../*|*/../*|*/..) echo '✗ Skill 压缩包包含非法路径'; exit 1 ;;
    esac
  done < <(unzip -Z1 "$PAYLOAD")
  unzip -oq "$PAYLOAD" -d "$ARCHIVE_DIR"
  MANIFESTS=("$ARCHIVE_DIR"/**/SKILL.md(N))
  if [ "\${#MANIFESTS[@]}" -ne 1 ]; then
    echo '✗ Skill 压缩包必须且只能包含一个 SKILL.md'
    exit 1
  fi
  SOURCE_ROOT="\${MANIFESTS[1]:h}"
fi
BACKUP_STAMP="$(date +%Y%m%d-%H%M%S)-$$"
for DEST in ${destinations}; do
  STAGE="$TEMP_DIR/stage"
  rm -rf "$STAGE"
  mkdir -p "$STAGE"
  if ${archive ? "true" : "false"}; then
    cp -R "$SOURCE_ROOT"/. "$STAGE"/
  else
    cp "$PAYLOAD" "$STAGE/SKILL.md"
  fi
  mkdir -p "\${DEST:h}"
  if [ -e "$DEST" ]; then
    mv "$DEST" "$DEST.skillport-backup-$BACKUP_STAMP"
  fi
  mv "$STAGE" "$DEST"
  echo "✓ 已安装到 $DEST"
done
echo '✓ SkillPort 安装完成（${paths.length} 个 AI 工具）'
`;
}

export function createWindowsInstaller(base64: string, extension: string, paths: string[]) {
  const destinations = paths.map((path) => `$env:USERPROFILE + "\\${path.replaceAll("/", "\\")}"`).join(", ");
  const archive = isArchiveExtension(extension);
  return `$ErrorActionPreference = "Stop"
$tempDir = Join-Path $env:TEMP "skillport-${Date.now()}"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
try {
  $file = Join-Path $tempDir "skill.${archive ? "zip" : "md"}"
  [IO.File]::WriteAllBytes($file, [Convert]::FromBase64String("${base64}"))
  $sourceRoot = $null
  if ($${archive ? "true" : "false"}) {
    $archiveDir = Join-Path $tempDir "archive"
    New-Item -ItemType Directory -Force -Path $archiveDir | Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($file)
    try {
      $archiveRoot = [IO.Path]::GetFullPath($archiveDir + [IO.Path]::DirectorySeparatorChar)
      foreach ($entry in $zip.Entries) {
        $entryPath = [IO.Path]::GetFullPath((Join-Path $archiveDir $entry.FullName))
        if (-not $entryPath.StartsWith($archiveRoot, [StringComparison]::OrdinalIgnoreCase)) {
          throw "Skill 压缩包包含非法路径"
        }
      }
    } finally { $zip.Dispose() }
    Expand-Archive -Path $file -DestinationPath $archiveDir -Force
    $manifests = @(Get-ChildItem -Path $archiveDir -Filter "SKILL.md" -File -Recurse)
    if ($manifests.Count -ne 1) { throw "Skill 压缩包必须且只能包含一个 SKILL.md" }
    $sourceRoot = $manifests[0].DirectoryName
  }
  $destinations = @(${destinations})
  $backupStamp = (Get-Date -Format "yyyyMMdd-HHmmss") + "-" + $PID
  foreach ($dest in $destinations) {
    $stage = Join-Path $tempDir "stage"
    Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $stage | Out-Null
    if ($${archive ? "true" : "false"}) {
      Get-ChildItem -Path $sourceRoot -Force | Copy-Item -Destination $stage -Recurse -Force
    } else {
      Copy-Item $file (Join-Path $stage "SKILL.md") -Force
    }
    New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
    if (Test-Path $dest) { Move-Item $dest ($dest + ".skillport-backup-" + $backupStamp) }
    Move-Item $stage $dest
    Write-Host "✓ 已安装到 $dest"
  }
  Write-Host "✓ SkillPort 安装完成（${paths.length} 个 AI 工具）"
} finally {
  Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
`;
}

export function createMacInstallerArchive(script: string, scriptFileName: string) {
  const readme = `SkillPort macOS 安装器\n\n1. 解压本文件。\n2. 双击 ${scriptFileName}。\n3. 如果 macOS 阻止打开，请右键安装器并选择“打开”。\n\n也可以在终端运行：zsh "${scriptFileName}"\n`;
  return zipSync({
    [scriptFileName]: [strToU8(script), { os: 3, attrs: 0o755 << 16 }],
    "安装说明.txt": [strToU8(readme), { os: 3, attrs: 0o644 << 16 }],
  }, { level: 6 });
}
