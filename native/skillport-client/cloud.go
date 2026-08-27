package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

type user struct {
	ID          string `json:"id"`
	Email       string `json:"email"`
	DisplayName string `json:"displayName"`
}

type skill struct {
	ID                string   `json:"id"`
	Name              string   `json:"name"`
	Description       string   `json:"description"`
	Category          string   `json:"category"`
	FileName          string   `json:"fileName"`
	SizeBytes         int64    `json:"sizeBytes"`
	SHA256            string   `json:"sha256"`
	Note              string   `json:"note"`
	ToolCompatibility string   `json:"toolCompatibility"`
	Compatible        []string `json:"compatible,omitempty"`
	Author            string   `json:"author,omitempty"`
	PullCount         int64    `json:"pullCount,omitempty"`
	Pulled            bool     `json:"pulled,omitempty"`
}

type cloudClient struct {
	store      *configStore
	httpClient *http.Client
	apiBase    string
	mu         sync.Mutex
}

func newCloudClient(store *configStore) (*cloudClient, error) {
	configuration, err := store.load()
	if err != nil {
		return nil, err
	}
	jar, err := cookiejar.New(nil)
	if err != nil {
		return nil, err
	}
	base, err := url.Parse(strings.TrimRight(configuration.APIBase, "/"))
	if err != nil {
		return nil, err
	}
	if configuration.SessionToken != "" {
		jar.SetCookies(base, []*http.Cookie{{
			Name: "skillport_session", Value: configuration.SessionToken,
			Path: "/", Secure: base.Scheme == "https", HttpOnly: true,
		}})
	}
	return &cloudClient{
		store: store,
		httpClient: &http.Client{
			Jar:     jar,
			Timeout: 45 * time.Second,
		},
		apiBase: strings.TrimRight(configuration.APIBase, "/"),
	}, nil
}

func (client *cloudClient) login(ctx context.Context, email, password string) (user, error) {
	var response struct {
		User user `json:"user"`
	}
	err := client.jsonRequest(ctx, http.MethodPost, "/api/auth/login", map[string]string{
		"email": strings.TrimSpace(email), "password": password,
	}, &response)
	if err != nil {
		return user{}, err
	}
	if err := client.persistSession(); err != nil {
		return user{}, err
	}
	return response.User, nil
}

func (client *cloudClient) logout(ctx context.Context) error {
	err := client.jsonRequest(ctx, http.MethodPost, "/api/auth/logout", nil, nil)
	configuration, loadErr := client.store.load()
	if loadErr == nil {
		configuration.SessionToken = ""
		loadErr = client.store.save(configuration)
	}
	if err != nil {
		return err
	}
	return loadErr
}

func (client *cloudClient) me(ctx context.Context) (user, error) {
	var response struct {
		User user `json:"user"`
	}
	err := client.jsonRequest(ctx, http.MethodGet, "/api/auth/me", nil, &response)
	return response.User, err
}

func (client *cloudClient) skills(ctx context.Context) ([]skill, error) {
	var response struct {
		Skills []skill `json:"skills"`
	}
	err := client.jsonRequest(ctx, http.MethodGet, "/api/skills", nil, &response)
	return response.Skills, err
}

func (client *cloudClient) publicSkills(ctx context.Context) ([]skill, error) {
	var response struct {
		Skills []skill `json:"skills"`
	}
	err := client.jsonRequest(ctx, http.MethodGet, "/api/public-skills", nil, &response)
	return response.Skills, err
}

func (client *cloudClient) pull(ctx context.Context, publicSkillID string) (skill, error) {
	var response struct {
		Skill skill `json:"skill"`
	}
	path := "/api/public-skills/" + url.PathEscape(publicSkillID) + "/pull"
	err := client.jsonRequest(ctx, http.MethodPost, path, nil, &response)
	return response.Skill, err
}

func (client *cloudClient) download(ctx context.Context, selected skill) (string, error) {
	request, err := http.NewRequestWithContext(ctx, http.MethodGet,
		client.apiBase+"/api/skills/"+url.PathEscape(selected.ID)+"/file", nil)
	if err != nil {
		return "", err
	}
	response, err := client.httpClient.Do(request)
	if err != nil {
		return "", err
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return "", cloudError(response)
	}
	configurationDirectory, err := os.UserConfigDir()
	if err != nil {
		return "", err
	}
	downloadDirectory := filepath.Join(configurationDirectory, "SkillPort", "downloads")
	if err := os.MkdirAll(downloadDirectory, 0o700); err != nil {
		return "", err
	}
	temporary, err := os.CreateTemp(downloadDirectory, "skill-*.download")
	if err != nil {
		return "", err
	}
	temporaryPath := temporary.Name()
	succeeded := false
	defer func() {
		_ = temporary.Close()
		if !succeeded {
			_ = os.Remove(temporaryPath)
		}
	}()
	written, err := io.Copy(temporary, io.LimitReader(response.Body, 26*1024*1024))
	if err != nil {
		return "", err
	}
	if selected.SizeBytes > 0 && written != selected.SizeBytes {
		return "", fmt.Errorf("Skill 文件大小校验失败")
	}
	if err := temporary.Sync(); err != nil {
		return "", err
	}
	succeeded = true
	return temporaryPath, nil
}

func (client *cloudClient) jsonRequest(ctx context.Context, method, path string, requestBody, responseBody any) error {
	client.mu.Lock()
	defer client.mu.Unlock()
	var body io.Reader
	if requestBody != nil {
		payload, err := json.Marshal(requestBody)
		if err != nil {
			return err
		}
		body = bytes.NewReader(payload)
	}
	request, err := http.NewRequestWithContext(ctx, method, client.apiBase+path, body)
	if err != nil {
		return err
	}
	if requestBody != nil {
		request.Header.Set("Content-Type", "application/json")
	}
	request.Header.Set("Accept", "application/json")
	request.Header.Set("User-Agent", "SkillPort-Client/"+appVersion)
	response, err := client.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return cloudError(response)
	}
	if responseBody == nil || response.StatusCode == http.StatusNoContent {
		return nil
	}
	if err := json.NewDecoder(io.LimitReader(response.Body, 4*1024*1024)).Decode(responseBody); err != nil {
		return fmt.Errorf("云端返回内容无法解析: %w", err)
	}
	return nil
}

func (client *cloudClient) persistSession() error {
	base, err := url.Parse(client.apiBase)
	if err != nil {
		return err
	}
	var token string
	for _, cookie := range client.httpClient.Jar.Cookies(base) {
		if cookie.Name == "skillport_session" {
			token = cookie.Value
			break
		}
	}
	if token == "" {
		return fmt.Errorf("云端没有返回登录会话")
	}
	configuration, err := client.store.load()
	if err != nil {
		return err
	}
	configuration.APIBase = client.apiBase
	configuration.SessionToken = token
	return client.store.save(configuration)
}

func cloudError(response *http.Response) error {
	payload, _ := io.ReadAll(io.LimitReader(response.Body, 64*1024))
	var message struct {
		Error   string `json:"error"`
		Detail  string `json:"detail"`
		Message string `json:"message"`
	}
	_ = json.Unmarshal(payload, &message)
	for _, value := range []string{message.Error, message.Detail, message.Message} {
		if strings.TrimSpace(value) != "" {
			return fmt.Errorf("%s", strings.TrimSpace(value))
		}
	}
	if response.StatusCode == http.StatusUnauthorized {
		return fmt.Errorf("登录已失效，请重新登录")
	}
	return fmt.Errorf("云端请求失败（HTTP %d）", response.StatusCode)
}
