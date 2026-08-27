package main

import (
	"archive/zip"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"unicode"
)

var toolDirectories = map[string]string{
	"codex":    ".codex/skills",
	"qoder":    ".qoder/skills",
	"opencode": ".config/opencode/skills",
	"claude":   ".claude/skills",
}

type detectedTool struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	Directory string `json:"directory"`
	Detected  bool   `json:"detected"`
}

func detectTools() ([]detectedTool, error) {
	home, err := os.UserHomeDir()
	if err != nil {
		return nil, err
	}
	labels := map[string]string{
		"codex": "Codex", "qoder": "Qoder", "opencode": "OpenCode", "claude": "Claude Code",
	}
	order := []string{"codex", "qoder", "opencode", "claude"}
	result := make([]detectedTool, 0, len(order))
	for _, id := range order {
		directory := filepath.Join(home, filepath.FromSlash(toolDirectories[id]))
		_, statErr := os.Stat(filepath.Dir(directory))
		result = append(result, detectedTool{
			ID: id, Name: labels[id], Directory: directory, Detected: statErr == nil,
		})
	}
	return result, nil
}

func installSkill(archivePath string, selected skill, targets []string) error {
	if len(targets) == 0 {
		return fmt.Errorf("请至少选择一个 AI 工具")
	}
	if err := verifyFileSHA256(archivePath, selected.SHA256); err != nil {
		return err
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return err
	}
	slug := skillSlug(selected.Name)
	for _, target := range uniqueTargets(targets) {
		destination, resolveErr := targetPath(home, target, slug)
		if resolveErr != nil {
			return resolveErr
		}
		if extractErr := installArchiveAtomically(archivePath, selected.FileName, destination); extractErr != nil {
			return extractErr
		}
	}
	return nil
}

func uninstallSkill(skillName string, targets []string) (int, error) {
	home, err := os.UserHomeDir()
	if err != nil {
		return 0, err
	}
	removed := 0
	for _, target := range uniqueTargets(targets) {
		destination, resolveErr := targetPath(home, target, skillSlug(skillName))
		if resolveErr != nil {
			return removed, resolveErr
		}
		if _, statErr := os.Lstat(destination); statErr == nil {
			if removeErr := os.RemoveAll(destination); removeErr != nil {
				return removed, fmt.Errorf("无法卸载 %s: %w", target, removeErr)
			}
			removed++
		} else if !os.IsNotExist(statErr) {
			return removed, statErr
		}
	}
	return removed, nil
}

func targetPath(home, target, slug string) (string, error) {
	relative, ok := toolDirectories[target]
	if !ok {
		return "", fmt.Errorf("不支持的 AI 工具: %s", target)
	}
	homePath, err := filepath.Abs(home)
	if err != nil {
		return "", err
	}
	destination, err := filepath.Abs(filepath.Join(homePath, filepath.FromSlash(relative), slug))
	if err != nil {
		return "", err
	}
	prefix := homePath + string(os.PathSeparator)
	if !strings.HasPrefix(destination, prefix) {
		return "", fmt.Errorf("无效的安装路径")
	}
	return destination, nil
}

func installArchiveAtomically(source, fileName, destination string) error {
	parent := filepath.Dir(destination)
	if err := os.MkdirAll(parent, 0o700); err != nil {
		return err
	}
	temporary, err := os.MkdirTemp(parent, ".skillport-install-*")
	if err != nil {
		return err
	}
	defer os.RemoveAll(temporary)
	lowerName := strings.ToLower(fileName)
	if strings.HasSuffix(lowerName, ".zip") || strings.HasSuffix(lowerName, ".skill") {
		if err := extractZipSafely(source, temporary); err != nil {
			return err
		}
	} else {
		if err := copyFile(source, filepath.Join(temporary, "SKILL.md"), 0o600); err != nil {
			return err
		}
	}
	if _, err := os.Stat(filepath.Join(temporary, "SKILL.md")); err != nil {
		return fmt.Errorf("Skill 包根目录缺少 SKILL.md")
	}
	if err := os.RemoveAll(destination); err != nil {
		return err
	}
	if err := os.Rename(temporary, destination); err != nil {
		return err
	}
	return nil
}

func extractZipSafely(source, destination string) error {
	reader, err := zip.OpenReader(source)
	if err != nil {
		return fmt.Errorf("Skill 压缩包无法读取: %w", err)
	}
	defer reader.Close()
	rootPrefix := commonZipRoot(reader.File)
	var totalUncompressed uint64
	for _, entry := range reader.File {
		rawName := strings.ReplaceAll(entry.Name, "\\", "/")
		if strings.HasPrefix(rawName, "/") || containsParentSegment(rawName) {
			return fmt.Errorf("Skill 压缩包包含非法路径")
		}
		name := strings.TrimPrefix(rawName, rootPrefix)
		name = strings.TrimPrefix(name, "/")
		if name == "" {
			continue
		}
		if entry.Mode()&os.ModeSymlink != 0 {
			return fmt.Errorf("Skill 压缩包不能包含符号链接")
		}
		totalUncompressed += entry.UncompressedSize64
		if totalUncompressed > 100*1024*1024 {
			return fmt.Errorf("Skill 解压后不能超过 100MB")
		}
		target := filepath.Join(destination, filepath.FromSlash(name))
		normalizedDestination, _ := filepath.Abs(destination)
		normalizedTarget, _ := filepath.Abs(target)
		if !strings.HasPrefix(normalizedTarget, normalizedDestination+string(os.PathSeparator)) {
			return fmt.Errorf("Skill 压缩包包含非法路径")
		}
		if entry.FileInfo().IsDir() {
			if err := os.MkdirAll(normalizedTarget, 0o700); err != nil {
				return err
			}
			continue
		}
		if err := os.MkdirAll(filepath.Dir(normalizedTarget), 0o700); err != nil {
			return err
		}
		input, err := entry.Open()
		if err != nil {
			return err
		}
		mode := os.FileMode(0o600)
		if entry.Mode()&0o111 != 0 {
			mode = 0o700
		}
		output, err := os.OpenFile(normalizedTarget, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, mode)
		if err != nil {
			input.Close()
			return err
		}
		_, copyErr := io.Copy(output, io.LimitReader(input, 100*1024*1024))
		closeErr := output.Close()
		input.Close()
		if copyErr != nil {
			return copyErr
		}
		if closeErr != nil {
			return closeErr
		}
	}
	return nil
}

func containsParentSegment(value string) bool {
	for _, segment := range strings.Split(value, "/") {
		if segment == ".." {
			return true
		}
	}
	return false
}

func commonZipRoot(files []*zip.File) string {
	var root string
	for _, file := range files {
		name := strings.TrimPrefix(strings.ReplaceAll(file.Name, "\\", "/"), "/")
		parts := strings.Split(name, "/")
		if len(parts) < 2 || parts[0] == "" {
			return ""
		}
		if root == "" {
			root = parts[0]
		} else if root != parts[0] {
			return ""
		}
	}
	if root == "" {
		return ""
	}
	return root + "/"
}

func verifyFileSHA256(path, expected string) error {
	file, err := os.Open(path)
	if err != nil {
		return err
	}
	defer file.Close()
	digest := sha256.New()
	if _, err := io.Copy(digest, file); err != nil {
		return err
	}
	actual := hex.EncodeToString(digest.Sum(nil))
	if !strings.EqualFold(actual, strings.TrimSpace(expected)) {
		return fmt.Errorf("Skill 文件 SHA-256 校验失败")
	}
	return nil
}

func copyFile(source, destination string, mode os.FileMode) error {
	input, err := os.Open(source)
	if err != nil {
		return err
	}
	defer input.Close()
	output, err := os.OpenFile(destination, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, mode)
	if err != nil {
		return err
	}
	_, copyErr := io.Copy(output, input)
	closeErr := output.Close()
	if copyErr != nil {
		return copyErr
	}
	return closeErr
}

func skillSlug(value string) string {
	var builder strings.Builder
	lastHyphen := false
	for _, character := range strings.ToLower(strings.TrimSpace(value)) {
		if unicode.IsLetter(character) || unicode.IsNumber(character) {
			builder.WriteRune(character)
			lastHyphen = false
		} else if builder.Len() > 0 && !lastHyphen {
			builder.WriteByte('-')
			lastHyphen = true
		}
	}
	returnValue := strings.Trim(builder.String(), "-")
	if returnValue == "" {
		return "skillport-skill"
	}
	return returnValue
}

func uniqueTargets(targets []string) []string {
	seen := make(map[string]bool)
	result := make([]string, 0, len(targets))
	for _, target := range targets {
		if _, valid := toolDirectories[target]; valid && !seen[target] {
			seen[target] = true
			result = append(result, target)
		}
	}
	sort.Strings(result)
	return result
}
