package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
)

func installWindowsClient() error {
	if runtime.GOOS != "windows" {
		return fmt.Errorf("Windows 安装器只能在 Windows 中运行")
	}
	localAppData := os.Getenv("LOCALAPPDATA")
	if localAppData == "" {
		return fmt.Errorf("无法读取 LOCALAPPDATA")
	}
	installDirectory := filepath.Join(localAppData, "SkillPort")
	if err := os.MkdirAll(installDirectory, 0o700); err != nil {
		return err
	}
	source, err := os.Executable()
	if err != nil {
		return err
	}
	destination := filepath.Join(installDirectory, "SkillPort.exe")
	if !samePath(source, destination) {
		payload, readErr := os.ReadFile(source)
		if readErr != nil {
			return readErr
		}
		if writeErr := os.WriteFile(destination, payload, 0o700); writeErr != nil {
			return writeErr
		}
	}
	desktop := filepath.Join(os.Getenv("USERPROFILE"), "Desktop", "SkillPort.lnk")
	shortcutScript := fmt.Sprintf(
		"$w=New-Object -ComObject WScript.Shell;$s=$w.CreateShortcut('%s');$s.TargetPath='%s';$s.Arguments='--client';$s.WorkingDirectory='%s';$s.Description='SkillPort 客户端';$s.Save()",
		powerShellLiteral(desktop), powerShellLiteral(destination), powerShellLiteral(installDirectory))
	if output, commandErr := exec.Command("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", shortcutScript).CombinedOutput(); commandErr != nil {
		return fmt.Errorf("创建桌面快捷方式失败: %s", strings.TrimSpace(string(output)))
	}
	if err := exec.Command(destination, "--client").Start(); err != nil {
		return err
	}
	showWindowsMessage("SkillPort", "安装完成。SkillPort 客户端已经打开，桌面快捷方式可随时再次启动。")
	return nil
}

func samePath(left, right string) bool {
	leftPath, leftErr := filepath.Abs(left)
	rightPath, rightErr := filepath.Abs(right)
	return leftErr == nil && rightErr == nil && strings.EqualFold(leftPath, rightPath)
}

func powerShellLiteral(value string) string {
	return strings.ReplaceAll(value, "'", "''")
}

func showWindowsMessage(title, message string) {
	if runtime.GOOS != "windows" {
		return
	}
	script := fmt.Sprintf("Add-Type -AssemblyName PresentationFramework;[System.Windows.MessageBox]::Show('%s','%s') | Out-Null",
		powerShellLiteral(message), powerShellLiteral(title))
	_ = exec.Command("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script).Run()
}
