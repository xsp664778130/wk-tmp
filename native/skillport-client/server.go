package main

import (
	"embed"
	"encoding/json"
	"fmt"
	"io"
	"io/fs"
	"net/http"
	"os"
	"runtime"
	"strings"
	"sync"
)

//go:embed ui/*
var uiFiles embed.FS

type application struct {
	cloud      *cloudClient
	store      *configStore
	localToken string
	shutdown   func()
	mu         sync.Mutex
}

type applicationState struct {
	Authenticated bool           `json:"authenticated"`
	User          *user          `json:"user,omitempty"`
	Skills        []skill        `json:"skills"`
	PublicSkills  []skill        `json:"publicSkills"`
	Tools         []detectedTool `json:"tools"`
	Version       string         `json:"version"`
	OS            string         `json:"os"`
}

func newApplication(cloud *cloudClient, store *configStore, localToken string) *application {
	return &application{cloud: cloud, store: store, localToken: localToken}
}

func (application *application) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", application.health)
	mux.HandleFunc("GET /", application.index)
	assets, _ := fs.Sub(uiFiles, "ui")
	mux.Handle("GET /assets/", http.StripPrefix("/assets/", http.FileServer(http.FS(assets))))
	mux.Handle("GET /api/state", application.localOnly(http.HandlerFunc(application.state)))
	mux.Handle("POST /api/login", application.localOnly(http.HandlerFunc(application.login)))
	mux.Handle("POST /api/logout", application.localOnly(http.HandlerFunc(application.logout)))
	mux.Handle("POST /api/pull", application.localOnly(http.HandlerFunc(application.pull)))
	mux.Handle("POST /api/install", application.localOnly(http.HandlerFunc(application.install)))
	mux.Handle("POST /api/uninstall", application.localOnly(http.HandlerFunc(application.uninstall)))
	return securityHeaders(mux)
}

func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		response.Header().Set("X-Content-Type-Options", "nosniff")
		response.Header().Set("Referrer-Policy", "no-referrer")
		response.Header().Set("Cache-Control", "no-store")
		response.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'")
		next.ServeHTTP(response, request)
	})
}

func (application *application) localOnly(next http.Handler) http.Handler {
	return http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		if request.Header.Get("X-SkillPort-Local") != application.localToken {
			writeError(response, http.StatusForbidden, "本机客户端校验失败，请关闭页面后重新打开客户端")
			return
		}
		next.ServeHTTP(response, request)
	})
}

func (application *application) health(response http.ResponseWriter, _ *http.Request) {
	response.Header().Set("X-SkillPort-Client", "1")
	response.WriteHeader(http.StatusOK)
}

func (application *application) index(response http.ResponseWriter, _ *http.Request) {
	payload, err := uiFiles.ReadFile("ui/index.html")
	if err != nil {
		writeError(response, http.StatusInternalServerError, "客户端界面读取失败")
		return
	}
	payload = []byte(strings.ReplaceAll(string(payload), "__SKILLPORT_LOCAL_TOKEN__", application.localToken))
	response.Header().Set("Content-Type", "text/html; charset=utf-8")
	_, _ = response.Write(payload)
}

func (application *application) state(response http.ResponseWriter, request *http.Request) {
	application.mu.Lock()
	defer application.mu.Unlock()
	tools, err := detectTools()
	if err != nil {
		writeError(response, http.StatusInternalServerError, err.Error())
		return
	}
	currentUser, err := application.cloud.me(request.Context())
	if err != nil {
		writeJSON(response, http.StatusOK, applicationState{
			Skills: []skill{}, PublicSkills: []skill{}, Tools: tools, Version: appVersion, OS: runtime.GOOS,
		})
		return
	}
	privateSkills, err := application.cloud.skills(request.Context())
	if err != nil {
		writeError(response, http.StatusBadGateway, err.Error())
		return
	}
	publicSkills, err := application.cloud.publicSkills(request.Context())
	if err != nil {
		writeError(response, http.StatusBadGateway, err.Error())
		return
	}
	writeJSON(response, http.StatusOK, applicationState{
		Authenticated: true, User: &currentUser, Skills: privateSkills, PublicSkills: publicSkills,
		Tools: tools, Version: appVersion, OS: runtime.GOOS,
	})
}

func (application *application) login(response http.ResponseWriter, request *http.Request) {
	var input struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	if err := decodeJSON(request, &input); err != nil {
		writeError(response, http.StatusBadRequest, err.Error())
		return
	}
	currentUser, err := application.cloud.login(request.Context(), input.Email, input.Password)
	if err != nil {
		writeError(response, http.StatusUnauthorized, err.Error())
		return
	}
	writeJSON(response, http.StatusOK, map[string]any{"user": currentUser})
}

func (application *application) logout(response http.ResponseWriter, request *http.Request) {
	if err := application.cloud.logout(request.Context()); err != nil {
		writeError(response, http.StatusBadGateway, err.Error())
		return
	}
	response.WriteHeader(http.StatusNoContent)
}

func (application *application) pull(response http.ResponseWriter, request *http.Request) {
	var input struct {
		PublicSkillID string `json:"publicSkillId"`
	}
	if err := decodeJSON(request, &input); err != nil || strings.TrimSpace(input.PublicSkillID) == "" {
		writeError(response, http.StatusBadRequest, "请选择要拉取的 Skill")
		return
	}
	pulled, err := application.cloud.pull(request.Context(), input.PublicSkillID)
	if err != nil {
		writeError(response, http.StatusBadGateway, err.Error())
		return
	}
	writeJSON(response, http.StatusOK, map[string]any{"skill": pulled})
}

func (application *application) install(response http.ResponseWriter, request *http.Request) {
	var input struct {
		SkillID string   `json:"skillId"`
		Targets []string `json:"targets"`
	}
	if err := decodeJSON(request, &input); err != nil {
		writeError(response, http.StatusBadRequest, err.Error())
		return
	}
	privateSkills, err := application.cloud.skills(request.Context())
	if err != nil {
		writeError(response, http.StatusBadGateway, err.Error())
		return
	}
	selected, found := findSkill(privateSkills, input.SkillID)
	if !found {
		writeError(response, http.StatusNotFound, "Skill 不在你的个人空间中")
		return
	}
	archivePath, err := application.cloud.download(request.Context(), selected)
	if err != nil {
		writeError(response, http.StatusBadGateway, err.Error())
		return
	}
	defer os.Remove(archivePath)
	if err := installSkill(archivePath, selected, input.Targets); err != nil {
		writeError(response, http.StatusInternalServerError, err.Error())
		return
	}
	writeJSON(response, http.StatusOK, map[string]any{"message": fmt.Sprintf("%s 已安装到 %d 个工具", selected.Name, len(uniqueTargets(input.Targets)))})
}

func (application *application) uninstall(response http.ResponseWriter, request *http.Request) {
	var input struct {
		SkillName string   `json:"skillName"`
		Targets   []string `json:"targets"`
	}
	if err := decodeJSON(request, &input); err != nil || strings.TrimSpace(input.SkillName) == "" {
		writeError(response, http.StatusBadRequest, "请选择要卸载的 Skill")
		return
	}
	removed, err := uninstallSkill(input.SkillName, input.Targets)
	if err != nil {
		writeError(response, http.StatusInternalServerError, err.Error())
		return
	}
	message := "本机未找到对应 Skill，无需卸载"
	if removed > 0 {
		message = fmt.Sprintf("已从 %d 个工具永久删除本机副本", removed)
	}
	writeJSON(response, http.StatusOK, map[string]any{"message": message})
}

func decodeJSON(request *http.Request, target any) error {
	decoder := json.NewDecoder(io.LimitReader(request.Body, 128*1024))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(target); err != nil {
		return fmt.Errorf("请求内容无效")
	}
	return nil
}

func findSkill(skills []skill, skillID string) (skill, bool) {
	for _, item := range skills {
		if item.ID == skillID {
			return item, true
		}
	}
	return skill{}, false
}

func writeJSON(response http.ResponseWriter, status int, value any) {
	response.Header().Set("Content-Type", "application/json; charset=utf-8")
	response.WriteHeader(status)
	_ = json.NewEncoder(response).Encode(value)
}

func writeError(response http.ResponseWriter, status int, message string) {
	writeJSON(response, status, map[string]string{"error": message})
}
