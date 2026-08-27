package main

import (
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"
)

const (
	appVersion     = "1.0.0"
	defaultAPIBase = "https://www.jmuyuer.com"
	localAddress   = "127.0.0.1:32145"
)

func main() {
	if runtime.GOOS == "windows" && !containsArg("--client") && strings.Contains(strings.ToLower(filepath.Base(os.Args[0])), "setup") {
		if err := installWindowsClient(); err != nil {
			_ = writeLocalLog("安装失败: " + err.Error())
			showWindowsMessage("SkillPort 安装失败", err.Error())
			os.Exit(1)
		}
		return
	}

	listener, err := net.Listen("tcp", localAddress)
	if err != nil {
		if existingClientAvailable() {
			_ = openBrowser("http://" + localAddress)
			return
		}
		fatal(err)
	}
	defer listener.Close()

	localToken, err := randomToken(24)
	if err != nil {
		fatal(err)
	}
	configStore, err := newConfigStore()
	if err != nil {
		fatal(err)
	}
	cloudClient, err := newCloudClient(configStore)
	if err != nil {
		fatal(err)
	}
	application := newApplication(cloudClient, configStore, localToken)
	server := &http.Server{
		Handler:           application.routes(),
		ReadHeaderTimeout: 5 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	if !containsArg("--no-open") {
		go func() {
			time.Sleep(250 * time.Millisecond)
			_ = openBrowser("http://" + localAddress)
		}()
	}
	if err := server.Serve(listener); err != nil && !errors.Is(err, http.ErrServerClosed) {
		fatal(err)
	}
}

func containsArg(expected string) bool {
	for _, value := range os.Args[1:] {
		if value == expected {
			return true
		}
	}
	return false
}

func randomToken(byteCount int) (string, error) {
	buffer := make([]byte, byteCount)
	if _, err := rand.Read(buffer); err != nil {
		return "", err
	}
	return hex.EncodeToString(buffer), nil
}

func existingClientAvailable() bool {
	client := &http.Client{Timeout: 800 * time.Millisecond}
	response, err := client.Get("http://" + localAddress + "/health")
	if err != nil {
		return false
	}
	defer response.Body.Close()
	return response.StatusCode == http.StatusOK && response.Header.Get("X-SkillPort-Client") == "1"
}

func openBrowser(url string) error {
	var command *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		command = exec.Command("rundll32", "url.dll,FileProtocolHandler", url)
	case "darwin":
		command = exec.Command("open", url)
	default:
		command = exec.Command("xdg-open", url)
	}
	return command.Start()
}

func fatal(err error) {
	_ = writeLocalLog("启动失败: " + err.Error())
	fmt.Fprintln(os.Stderr, "SkillPort Client:", err)
	os.Exit(1)
}

func writeLocalLog(message string) error {
	directory, err := os.UserConfigDir()
	if err != nil {
		return err
	}
	logDirectory := filepath.Join(directory, "SkillPort", "logs")
	if err := os.MkdirAll(logDirectory, 0o700); err != nil {
		return err
	}
	file, err := os.OpenFile(filepath.Join(logDirectory, "client.log"), os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o600)
	if err != nil {
		return err
	}
	defer file.Close()
	_, err = fmt.Fprintf(file, "%s %s\n", time.Now().Format(time.RFC3339), message)
	return err
}
