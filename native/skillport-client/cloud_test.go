package main

import (
	"context"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestCloudLoginPersistsSessionAndLoadsSkills(t *testing.T) {
	transport := roundTripFunc(func(request *http.Request) (*http.Response, error) {
		header := make(http.Header)
		status := http.StatusOK
		body := ""
		switch request.URL.Path {
		case "/api/auth/login":
			header.Add("Set-Cookie", "skillport_session=session-token; Path=/; HttpOnly; Secure")
			body = `{"user":{"id":"u1","email":"test@example.com","displayName":"Tester"}}`
		case "/api/skills":
			cookie, err := request.Cookie("skillport_session")
			if err != nil || cookie.Value != "session-token" {
				status = http.StatusUnauthorized
				body = `{"error":"unauthorized"}`
			} else {
				body = `{"skills":[{"id":"s1","name":"sample","fileName":"sample.zip"}]}`
			}
		default:
			status = http.StatusNotFound
			body = `{"error":"not found"}`
		}
		return &http.Response{StatusCode: status, Header: header,
			Body: io.NopCloser(strings.NewReader(body)), Request: request}, nil
	})

	home := t.TempDir()
	t.Setenv("HOME", home)
	store := &configStore{path: filepath.Join(home, "config.json")}
	if err := store.save(clientConfig{APIBase: "https://skillport.test"}); err != nil {
		t.Fatal(err)
	}
	client, err := newCloudClient(store)
	if err != nil {
		t.Fatal(err)
	}
	client.httpClient.Transport = transport
	loggedIn, err := client.login(context.Background(), "test@example.com", "password1")
	if err != nil {
		t.Fatal(err)
	}
	if loggedIn.DisplayName != "Tester" {
		t.Fatalf("unexpected user: %+v", loggedIn)
	}
	configuration, err := store.load()
	if err != nil || configuration.SessionToken != "session-token" {
		t.Fatalf("session was not persisted: %+v err=%v", configuration, err)
	}
	skills, err := client.skills(context.Background())
	if err != nil || len(skills) != 1 || skills[0].ID != "s1" {
		t.Fatalf("unexpected skills: %+v err=%v", skills, err)
	}
	info, err := os.Stat(store.path)
	if err != nil {
		t.Fatal(err)
	}
	if info.Mode().Perm()&0o077 != 0 {
		t.Fatalf("config permissions are too broad: %o", info.Mode().Perm())
	}
}

type roundTripFunc func(*http.Request) (*http.Response, error)

func (function roundTripFunc) RoundTrip(request *http.Request) (*http.Response, error) {
	return function(request)
}
