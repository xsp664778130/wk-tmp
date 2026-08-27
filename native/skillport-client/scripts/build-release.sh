#!/bin/bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
go_bin="${GO_BIN:-go}"
output_dir="${OUTPUT_DIR:-$project_dir/build/release}"
version="${VERSION:-1.0.0}"
mkdir -p "$project_dir/build" "$output_dir"
build_dir="$(mktemp -d "$project_dir/build/work.XXXXXX")"
trap 'rm -rf "$build_dir"' EXIT

rm -f "$output_dir/SkillPort-Setup.exe" "$output_dir/SkillPort-Bridge.pkg" "$output_dir/SHA256SUMS.txt"

cd "$project_dir"
"$go_bin" test ./...

CGO_ENABLED=0 GOOS=windows GOARCH=amd64 "$go_bin" build -trimpath \
  -ldflags "-s -w -H=windowsgui" -o "$output_dir/SkillPort-Setup.exe" .

CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 "$go_bin" build -trimpath \
  -ldflags "-s -w" -o "$build_dir/SkillPort-arm64" .
CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 "$go_bin" build -trimpath \
  -ldflags "-s -w" -o "$build_dir/SkillPort-amd64" .

app_dir="$build_dir/macos-root/Applications/SkillPort.app/Contents"
mkdir -p "$app_dir/MacOS"
lipo -create "$build_dir/SkillPort-arm64" "$build_dir/SkillPort-amd64" -output "$app_dir/MacOS/SkillPort"
chmod 0755 "$app_dir/MacOS/SkillPort"
cp "$project_dir/packaging/macos/Info.plist" "$app_dir/Info.plist"
codesign --force --deep --sign - "$build_dir/macos-root/Applications/SkillPort.app"
chmod 0755 "$project_dir/packaging/macos/postinstall"
pkgbuild --root "$build_dir/macos-root" \
  --scripts "$project_dir/packaging/macos" \
  --identifier com.skillport.client \
  --version "$version" \
  --install-location / \
  "$output_dir/SkillPort-Bridge.pkg"

(cd "$output_dir" && LC_ALL=C LANG=C shasum -a 256 SkillPort-Setup.exe SkillPort-Bridge.pkg > SHA256SUMS.txt)
echo "$output_dir"
