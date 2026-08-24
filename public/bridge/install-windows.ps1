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
    $script:LastJavaDiagnostic = ""
    try {
        $StartInfo = New-Object System.Diagnostics.ProcessStartInfo
        $StartInfo.FileName = $JavaPath
        $StartInfo.Arguments = "-version"
        $StartInfo.UseShellExecute = $false
        $StartInfo.CreateNoWindow = $true
        $StartInfo.RedirectStandardOutput = $true
        $StartInfo.RedirectStandardError = $true
        $Process = New-Object System.Diagnostics.Process
        $Process.StartInfo = $StartInfo
        [void]$Process.Start()
        $VersionText = $Process.StandardOutput.ReadToEnd() + [Environment]::NewLine + $Process.StandardError.ReadToEnd()
        $Process.WaitForExit()
        $script:LastJavaDiagnostic = $VersionText.Trim()
        if ($Process.ExitCode -ne 0) { return 0 }
        if ($VersionText -match 'version "(?:1\.)?(\d+)') { return [int]$Matches[1] }
    } catch {
        $script:LastJavaDiagnostic = $_.Exception.Message
        return 0
    }
    return 0
}

function Get-JavaReleaseMajorVersion([string]$ReleasePath) {
    try {
        $ReleaseText = [IO.File]::ReadAllText($ReleasePath)
        if ($ReleaseText -match '(?m)^JAVA_VERSION="(?:1\.)?(\d+)') { return [int]$Matches[1] }
    } catch { return 0 }
    return 0
}

function Invoke-SkillPortDownload([string]$Uri, [string]$OutFile) {
    $CurlCommand = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($CurlCommand) {
        & $CurlCommand.Source -fL --retry 3 --connect-timeout 15 -A "Mozilla/5.0 SkillPort-Installer" $Uri -o $OutFile
        if ($LASTEXITCODE -ne 0) { throw "下载失败：$Uri" }
        return
    }

    $Client = New-Object Net.WebClient
    try {
        $Client.Headers["User-Agent"] = "Mozilla/5.0 SkillPort-Installer"
        $Client.DownloadFile($Uri, $OutFile)
    } finally {
        $Client.Dispose()
    }
}

function Install-PortableJava {
    $RuntimeArtifact = switch ($env:PROCESSOR_ARCHITECTURE) {
        "ARM64" { "temurin-jre21-windows-aarch64.zip" }
        "AMD64" { "temurin-jre21-windows-x64.zip" }
        default { throw "暂不支持此 Windows 架构：$env:PROCESSOR_ARCHITECTURE" }
    }
    $TempDir = Join-Path ([IO.Path]::GetTempPath()) ("skillport-java-" + [Guid]::NewGuid().ToString("N"))
    $Archive = Join-Path $TempDir "temurin-jre.zip"
    $ChecksumFile = Join-Path $TempDir "temurin-jre.zip.sha256"
    New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
    try {
        $DownloadUrl = "$ApiBaseUrl/bridge/runtime/$RuntimeArtifact"
        Invoke-SkillPortDownload "$DownloadUrl.sha256" $ChecksumFile
        $ExpectedHash = (([IO.File]::ReadAllText($ChecksumFile) -split "\s+")[0]).Trim().ToUpperInvariant()
        if ($ExpectedHash -notmatch '^[A-F0-9]{64}$') { throw "Java 校验值格式无效。" }

        Write-Host "正在从 SkillPort 主站下载专用 Java 21（只保存到 .skillport）…"
        $Verified = $false
        $ActualHash = ""
        for ($Attempt = 1; $Attempt -le 3; $Attempt++) {
            Remove-Item -Path $Archive -Force -ErrorAction SilentlyContinue
            try {
                Invoke-SkillPortDownload $DownloadUrl $Archive
                $ActualHash = (Get-FileHash -Path $Archive -Algorithm SHA256).Hash
                if ($ExpectedHash -eq $ActualHash) {
                    $Verified = $true
                    break
                }
            } catch {
                if ($Attempt -eq 3) { throw }
            }
            Write-Host "Java 下载不完整，正在重试（$Attempt/3）…" -ForegroundColor Yellow
            Start-Sleep -Seconds 2
        }
        if (-not $Verified) {
            throw "Java 下载校验失败，已重试 3 次。期望：$ExpectedHash，实际：$ActualHash"
        }

        Remove-Item -Path $RuntimeDir -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null
        Expand-Archive -Path $Archive -DestinationPath $RuntimeDir -Force
        $Java = Get-ChildItem -Path $RuntimeDir -Filter "java.exe" -Recurse |
            Where-Object { $_.Directory.Name -eq "bin" } |
            Select-Object -First 1
        if (-not $Java) { throw "Java 21 压缩包中没有找到 bin\java.exe。" }

        $JavaHome = Split-Path (Split-Path $Java.FullName -Parent) -Parent
        $ReleaseFile = Join-Path $JavaHome "release"
        if (-not (Test-Path $ReleaseFile) -or (Get-JavaReleaseMajorVersion $ReleaseFile) -lt 21) {
            throw "Java 21 解压后的 release 文件不完整。"
        }

        $JavaMajorVersion = Get-JavaMajorVersion $Java.FullName
        if ($JavaMajorVersion -lt 21) {
            $Diagnostic = $script:LastJavaDiagnostic
            if (-not $Diagnostic) { $Diagnostic = "进程没有返回版本信息，请检查安全软件是否拦截 java.exe。" }
            throw "Java 21 已正确解压，但启动失败。路径：$($Java.FullName)。诊断：$Diagnostic"
        }
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
$BridgeChecksumFile = "$BridgeJar.sha256"
Invoke-SkillPortDownload "$ApiBaseUrl/bridge/skillport-bridge.jar" $BridgeJar
Invoke-SkillPortDownload "$ApiBaseUrl/bridge/skillport-bridge.jar.sha256" $BridgeChecksumFile
$ExpectedBridgeHash = (([IO.File]::ReadAllText($BridgeChecksumFile) -split "\s+")[0]).Trim().ToUpperInvariant()
$ActualBridgeHash = (Get-FileHash -Path $BridgeJar -Algorithm SHA256).Hash
if ($ExpectedBridgeHash -ne $ActualBridgeHash) { throw "Bridge 下载校验失败，已停止安装。" }

Write-Step 3 "绑定当前 SkillPort 账户"
$DeviceName = $env:COMPUTERNAME
$PairApiBaseUrl = $ApiBaseUrl
try {
    if (([Uri]$ApiBaseUrl).DnsSafeHost -eq "skillport-ai-workspace.mcbbss.chatgpt.site") {
        $PairApiBaseUrl = "https://www.jmuyuer.com"
        Write-Host "正在通过 SkillPort 主站安全入口完成配对…"
    }
} catch {
    throw "配对地址格式无效，请回到网站重新复制安装命令。"
}
& $JavaExe -jar $BridgeJar pair $PairApiBaseUrl $NettyUrl $PairingCode $DeviceName
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
