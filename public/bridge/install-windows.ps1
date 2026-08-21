param(
    [Parameter(Mandatory = $true)][string]$ApiBaseUrl,
    [Parameter(Mandatory = $true)][string]$NettyUrl,
    [Parameter(Mandatory = $true)][string]$PairingCode
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$ApiBaseUrl = $ApiBaseUrl.TrimEnd("/")
$InstallDir = Join-Path $env:USERPROFILE ".skillport"
$RuntimeDir = Join-Path $InstallDir "runtime"
$BridgeJar = Join-Path $InstallDir "skillport-bridge.jar"
$StartupDir = [Environment]::GetFolderPath("Startup")
$StartupFile = Join-Path $StartupDir "SkillPort Bridge.cmd"

function Write-Step([int]$Number, [string]$Message) {
    Write-Host ""
    Write-Host "[$Number/5] $Message" -ForegroundColor Cyan
}

function Get-JavaMajorVersion([string]$JavaPath) {
    try {
        $VersionText = (& $JavaPath -version 2>&1 | Out-String)
        if ($VersionText -match 'version "(?:1\.)?(\d+)') { return [int]$Matches[1] }
    } catch { return 0 }
    return 0
}

function Install-PortableJava {
    $Architecture = switch ($env:PROCESSOR_ARCHITECTURE) {
        "ARM64" { "aarch64" }
        "AMD64" { "x64" }
        default { throw "暂不支持此 Windows 架构：$env:PROCESSOR_ARCHITECTURE" }
    }
    $TempDir = Join-Path ([IO.Path]::GetTempPath()) ("skillport-java-" + [Guid]::NewGuid().ToString("N"))
    $Archive = Join-Path $TempDir "temurin-jre.zip"
    New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
    try {
        $ApiUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/$Architecture/jre/hotspot/normal/eclipse"
        try {
            $RedirectResponse = Invoke-WebRequest -Uri $ApiUrl -MaximumRedirection 0 -UseBasicParsing
            $DownloadUrl = $RedirectResponse.Headers.Location
        } catch {
            $DownloadUrl = $_.Exception.Response.Headers.Location
        }
        if (-not $DownloadUrl) { throw "无法获取 Java 下载地址。" }

        Write-Host "正在下载 SkillPort 专用 Java 21（只保存到 .skillport）…"
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $Archive -UseBasicParsing
        $ExpectedHash = ((Invoke-WebRequest -Uri "$DownloadUrl.sha256.txt" -UseBasicParsing).Content -split "\s+")[0]
        $ActualHash = (Get-FileHash -Path $Archive -Algorithm SHA256).Hash
        if ($ExpectedHash -ne $ActualHash) { throw "Java 下载校验失败，已停止安装。" }

        New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null
        Expand-Archive -Path $Archive -DestinationPath $RuntimeDir -Force
        $Java = Get-ChildItem -Path $RuntimeDir -Filter "java.exe" -Recurse | Select-Object -First 1
        if (-not $Java -or (Get-JavaMajorVersion $Java.FullName) -lt 21) { throw "Java 21 解压后无法运行。" }
        return $Java.FullName
    } finally {
        Remove-Item -Path $TempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "SkillPort Bridge 快速安装" -ForegroundColor Magenta
Write-Host "不会修改系统 Java，也不需要管理员权限。"
New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null

Write-Step 1 "检查运行环境"
$JavaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
if ($JavaCommand -and (Get-JavaMajorVersion $JavaCommand.Source) -ge 21) {
    $JavaExe = $JavaCommand.Source
    Write-Host "已找到可用的 Java $(Get-JavaMajorVersion $JavaExe)。"
} else {
    $JavaExe = Install-PortableJava
}

Write-Step 2 "下载并校验 SkillPort Bridge"
Invoke-WebRequest -Uri "$ApiBaseUrl/bridge/skillport-bridge.jar" -OutFile $BridgeJar -UseBasicParsing
$ExpectedBridgeHash = ((Invoke-WebRequest -Uri "$ApiBaseUrl/bridge/skillport-bridge.jar.sha256" -UseBasicParsing).Content -split "\s+")[0]
$ActualBridgeHash = (Get-FileHash -Path $BridgeJar -Algorithm SHA256).Hash
if ($ExpectedBridgeHash -ne $ActualBridgeHash) { throw "Bridge 下载校验失败，已停止安装。" }

Write-Step 3 "绑定当前 SkillPort 账户"
$DeviceName = $env:COMPUTERNAME
& $JavaExe -jar $BridgeJar pair $ApiBaseUrl $NettyUrl $PairingCode $DeviceName
if ($LASTEXITCODE -ne 0) { throw "设备配对失败，请回到网站刷新配对码后重试。" }

Write-Step 4 "设置登录后自动连接"
$JavawExe = Join-Path (Split-Path $JavaExe) "javaw.exe"
if (-not (Test-Path $JavawExe)) { $JavawExe = $JavaExe }
$StartupCommand = '@echo off' + [Environment]::NewLine + 'start "" /min "' + $JavawExe + '" -jar "' + $BridgeJar + '"'
[IO.File]::WriteAllText($StartupFile, $StartupCommand, [Text.Encoding]::ASCII)
Start-Process -FilePath $JavawExe -ArgumentList @("-jar", $BridgeJar) -WindowStyle Hidden

Write-Step 5 "安装完成"
Write-Host "SkillPort Bridge 已连接，并会在你登录 Windows 后自动启动。" -ForegroundColor Green
Write-Host "现在可以关闭此窗口，回到 SkillPort 网页。"
