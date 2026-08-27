package main

import (
	"archive/zip"
	"crypto/sha256"
	"encoding/hex"
	"os"
	"path/filepath"
	"testing"
)

func TestSkillSlug(t *testing.T) {
	if actual := skillSlug("  Release JDK21 / 排查  "); actual != "release-jdk21-排查" {
		t.Fatalf("unexpected slug: %s", actual)
	}
	if actual := skillSlug("---"); actual != "skillport-skill" {
		t.Fatalf("unexpected fallback slug: %s", actual)
	}
}

func TestSupportedToolPaths(t *testing.T) {
	home := t.TempDir()
	expected := map[string]string{
		"codex":    filepath.Join(home, ".codex", "skills", "demo"),
		"qoder":    filepath.Join(home, ".qoder", "skills", "demo"),
		"opencode": filepath.Join(home, ".config", "opencode", "skills", "demo"),
		"claude":   filepath.Join(home, ".claude", "skills", "demo"),
	}
	for target, want := range expected {
		actual, err := targetPath(home, target, "demo")
		if err != nil {
			t.Fatalf("target %s: %v", target, err)
		}
		if actual != want {
			t.Fatalf("target %s: got %s, want %s", target, actual, want)
		}
	}
}

func TestInstallZipStripsCommonRootAndUninstallsWithoutBackup(t *testing.T) {
	home := t.TempDir()
	t.Setenv("HOME", home)
	archive := filepath.Join(t.TempDir(), "sample.zip")
	writeTestZip(t, archive, map[string]string{
		"sample-skill/SKILL.md":         "---\nname: sample-skill\ndescription: test\n---\n",
		"sample-skill/scripts/check.sh": "#!/bin/sh\necho ok\n",
	})
	digest := fileDigest(t, archive)
	selected := skill{Name: "sample-skill", FileName: "sample.zip", SHA256: digest}
	if err := installSkill(archive, selected, []string{"codex", "qoder"}); err != nil {
		t.Fatal(err)
	}
	for _, target := range []string{".codex", ".qoder"} {
		manifest := filepath.Join(home, target, "skills", "sample-skill", "SKILL.md")
		if _, err := os.Stat(manifest); err != nil {
			t.Fatalf("manifest missing at %s: %v", manifest, err)
		}
	}
	removed, err := uninstallSkill("sample-skill", []string{"codex", "qoder"})
	if err != nil || removed != 2 {
		t.Fatalf("removed=%d err=%v", removed, err)
	}
	if backups, _ := filepath.Glob(filepath.Join(home, ".skillport", "backups", "*")); len(backups) != 0 {
		t.Fatalf("unexpected backups: %v", backups)
	}
}

func TestZipTraversalIsRejected(t *testing.T) {
	archive := filepath.Join(t.TempDir(), "unsafe.zip")
	writeTestZip(t, archive, map[string]string{"../outside/SKILL.md": "unsafe"})
	if err := extractZipSafely(archive, t.TempDir()); err == nil {
		t.Fatal("expected traversal archive to be rejected")
	}
}

func TestChecksumMismatchIsRejected(t *testing.T) {
	file := filepath.Join(t.TempDir(), "skill.md")
	if err := os.WriteFile(file, []byte("content"), 0o600); err != nil {
		t.Fatal(err)
	}
	if err := verifyFileSHA256(file, "0000"); err == nil {
		t.Fatal("expected checksum mismatch")
	}
}

func writeTestZip(t *testing.T, path string, entries map[string]string) {
	t.Helper()
	file, err := os.Create(path)
	if err != nil {
		t.Fatal(err)
	}
	writer := zip.NewWriter(file)
	for name, content := range entries {
		entry, createErr := writer.Create(name)
		if createErr != nil {
			t.Fatal(createErr)
		}
		if _, writeErr := entry.Write([]byte(content)); writeErr != nil {
			t.Fatal(writeErr)
		}
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
}

func fileDigest(t *testing.T, path string) string {
	t.Helper()
	payload, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	digest := sha256.Sum256(payload)
	return hex.EncodeToString(digest[:])
}
