$ErrorActionPreference = "Stop"
$JavaRoot = Split-Path -Parent $PSScriptRoot
Set-Location $JavaRoot
mvn -q -pl skillport-bridge -am -DskipTests package
$InputDir = Join-Path $JavaRoot "skillport-bridge\target\jpackage-input"
Remove-Item $InputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $InputDir | Out-Null
Copy-Item "skillport-bridge\target\skillport-bridge-1.0.0-SNAPSHOT.jar" $InputDir
jpackage --type msi --name SkillPortBridge --input $InputDir `
  --main-jar skillport-bridge-1.0.0-SNAPSHOT.jar --main-class com.skillport.bridge.SkillPortBridgeApplication `
  --dest "skillport-bridge\target\installer"
