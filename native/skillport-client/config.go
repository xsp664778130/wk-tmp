package main

import (
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"sync"
)

type clientConfig struct {
	APIBase      string `json:"apiBase"`
	SessionToken string `json:"sessionToken,omitempty"`
}

type configStore struct {
	path string
	mu   sync.Mutex
}

func newConfigStore() (*configStore, error) {
	directory, err := os.UserConfigDir()
	if err != nil {
		return nil, err
	}
	configDirectory := filepath.Join(directory, "SkillPort")
	if err := os.MkdirAll(configDirectory, 0o700); err != nil {
		return nil, err
	}
	return &configStore{path: filepath.Join(configDirectory, "config.json")}, nil
}

func (store *configStore) load() (clientConfig, error) {
	store.mu.Lock()
	defer store.mu.Unlock()
	configuration := clientConfig{APIBase: defaultAPIBase}
	payload, err := os.ReadFile(store.path)
	if errors.Is(err, os.ErrNotExist) {
		return configuration, nil
	}
	if err != nil {
		return configuration, err
	}
	if err := json.Unmarshal(payload, &configuration); err != nil {
		return clientConfig{}, err
	}
	if configuration.APIBase == "" {
		configuration.APIBase = defaultAPIBase
	}
	return configuration, nil
}

func (store *configStore) save(configuration clientConfig) error {
	store.mu.Lock()
	defer store.mu.Unlock()
	payload, err := json.MarshalIndent(configuration, "", "  ")
	if err != nil {
		return err
	}
	temporary := store.path + ".tmp"
	if err := os.WriteFile(temporary, payload, 0o600); err != nil {
		return err
	}
	if err := os.Chmod(temporary, 0o600); err != nil {
		return err
	}
	return os.Rename(temporary, store.path)
}
