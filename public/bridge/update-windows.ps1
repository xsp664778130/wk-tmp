param(
    [Parameter(Mandatory = $true)][string]$ApiBaseUrl
)

$ErrorActionPreference = "Stop"
$ApiBaseUrl = $ApiBaseUrl.TrimEnd("/")
$InstallDir = Join-Path $env:USERPROFILE ".skillport"
$BridgeJar = Join-Path $InstallDir "skillport-bridge.jar"
$StartupFile = Join-Path ([Environment]::GetFolderPath("Startup")) "SkillPortBridge.cmd"

if (-not (Test-Path $BridgeJar) -or -not (Test-Path $StartupFile)) {
    throw "尚未找到已安装的 SkillPort Bridge，请先在网站中完成配对。"
}

$TempDir = Join-Path ([IO.Path]::GetTempPath()) ("skillport-update-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
$NewJar = Join-Path $TempDir "skillport-bridge.jar"
$ChecksumFile = "$NewJar.sha256"

try {
    Write-Host "[1/3] 下载并校验最新版 Bridge" -ForegroundColor Cyan
    $Client = New-Object Net.WebClient
    $Client.Headers["User-Agent"] = "Mozilla/5.0 SkillPort-Updater"
    $Client.DownloadFile("$ApiBaseUrl/bridge/skillport-bridge.jar", $NewJar)
    $Client.DownloadFile("$ApiBaseUrl/bridge/skillport-bridge.jar.sha256", $ChecksumFile)
    $ExpectedHash = (([IO.File]::ReadAllText($ChecksumFile) -split "\s+")[0]).Trim().ToUpperInvariant()
    $ActualHash = (Get-FileHash -Path $NewJar -Algorithm SHA256).Hash
    if ($ExpectedHash -ne $ActualHash) { throw "Bridge 下载校验失败，已停止更新。" }

    Write-Host "[2/3] 重启 Bridge" -ForegroundColor Cyan
    $EscapedJar = [Regex]::Escape($BridgeJar)
    Get-CimInstance Win32_Process | Where-Object {
        $_.CommandLine -and $_.CommandLine -match $EscapedJar
    } | ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Milliseconds 800
    Move-Item -Path $NewJar -Destination $BridgeJar -Force
    [IO.File]::WriteAllText("$BridgeJar.sha256", "$ExpectedHash  skillport-bridge.jar`r`n", [Text.Encoding]::ASCII)
    Start-Process -FilePath $StartupFile -WindowStyle Hidden

    Write-Host "[3/3] 更新完成" -ForegroundColor Green
    Write-Host "SkillPort Bridge 已更新并重新连接，现在可以使用本机卸载。"
} finally {
    Remove-Item -Path $TempDir -Recurse -Force -ErrorAction SilentlyContinue
}
