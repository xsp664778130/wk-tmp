package com.skillport.bridge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ToolDetector {
    private final Path home;
    private final String osName;
    private final Map<String, String> environment;
    private final Path systemApplications;
    private final boolean includeSystemExecutableDirectories;

    public ToolDetector() {
        this(Path.of(System.getProperty("user.home")), System.getProperty("os.name", "unknown"), System.getenv(),
                Path.of("/Applications"), true);
    }

    ToolDetector(Path home, String osName, Map<String, String> environment, Path systemApplications) {
        this(home, osName, environment, systemApplications, false);
    }

    private ToolDetector(Path home, String osName, Map<String, String> environment, Path systemApplications,
                         boolean includeSystemExecutableDirectories) {
        this.home = home;
        this.osName = osName.toLowerCase(Locale.ROOT);
        this.environment = environment;
        this.systemApplications = systemApplications;
        this.includeSystemExecutableDirectories = includeSystemExecutableDirectories;
    }

    public List<String> detect() {
        List<String> installed = new ArrayList<>();
        if (isInstalled("codex", codexLocations())) installed.add("codex");
        if (isInstalled("qoder", qoderLocations())) installed.add("qoder");
        if (isInstalled("opencode", openCodeLocations())) installed.add("opencode");
        if (isInstalled("claude", claudeCodeLocations())) installed.add("claude");
        if (isInstalled("cursor", applicationLocations("Cursor"))) installed.add("cursor");
        return List.copyOf(installed);
    }

    private boolean isInstalled(String command, List<Path> knownLocations) {
        return knownLocations.stream().anyMatch(Files::exists) || executableExists(command);
    }

    private boolean executableExists(String command) {
        for (Path directory : executableDirectories()) {
            for (String fileName : executableNames(command)) {
                if (Files.isRegularFile(directory.resolve(fileName))) return true;
            }
        }
        return false;
    }

    private Set<Path> executableDirectories() {
        Set<Path> directories = new LinkedHashSet<>();
        String path = environmentValue("PATH");
        for (String value : path.split(java.io.File.pathSeparator)) {
            String normalized = value.trim().replaceAll("^\"|\"$", "");
            if (!normalized.isBlank()) directories.add(Path.of(normalized));
        }
        directories.add(home.resolve(".local/bin"));
        directories.add(home.resolve(".npm-global/bin"));
        directories.add(home.resolve(".bun/bin"));
        if (!includeSystemExecutableDirectories) return directories;
        if (isWindows()) {
            addEnvironmentPath(directories, "APPDATA", "npm");
            addEnvironmentPath(directories, "LOCALAPPDATA", "Programs");
        } else {
            directories.add(Path.of("/usr/local/bin"));
            directories.add(Path.of("/opt/homebrew/bin"));
        }
        return directories;
    }

    private List<String> executableNames(String command) {
        return isWindows()
                ? List.of(command + ".exe", command + ".cmd", command + ".bat", command)
                : List.of(command);
    }

    private List<Path> codexLocations() {
        List<Path> paths = applicationLocations("Codex");
        paths.add(home.resolve(".codex/config.toml"));
        paths.add(home.resolve(".codex/auth.json"));
        return paths;
    }

    private List<Path> qoderLocations() {
        List<Path> paths = new ArrayList<>();
        if (isWindows()) {
            addWindowsApplicationVariants(paths, "LOCALAPPDATA");
            addWindowsApplicationVariants(paths, "PROGRAMFILES");
            addWindowsApplicationVariants(paths, "PROGRAMFILES(X86)");
        } else {
            for (String applicationName : List.of("Qoder", "Qoder IDE", "Qoder CN")) {
                paths.add(systemApplications.resolve(applicationName + ".app"));
                paths.add(home.resolve("Applications").resolve(applicationName + ".app"));
            }
        }
        paths.add(home.resolve(".qoder/bin/qoder"));
        return paths;
    }

    private void addWindowsApplicationVariants(List<Path> paths, String environmentKey) {
        String root = environmentValue(environmentKey);
        if (root == null || root.isBlank()) return;
        Path base = Path.of(root);
        for (String directoryName : List.of("Qoder", "Qoder IDE", "Qoder CN")) {
            for (String executableName : List.of("Qoder.exe", "Qoder IDE.exe", "Qoder CN.exe")) {
                paths.add(base.resolve(directoryName).resolve(executableName));
                paths.add(base.resolve("Programs").resolve(directoryName).resolve(executableName));
            }
        }
    }

    private List<Path> openCodeLocations() {
        List<Path> paths = applicationLocations("OpenCode");
        paths.add(home.resolve(".opencode/bin/opencode"));
        paths.add(home.resolve(".config/opencode/opencode.json"));
        paths.add(home.resolve(".config/opencode/opencode.jsonc"));
        return paths;
    }

    private List<Path> claudeCodeLocations() {
        return List.of(
                home.resolve(".claude/local/claude"),
                home.resolve(".claude.json"),
                home.resolve(".claude/settings.json")
        );
    }

    private List<Path> applicationLocations(String applicationName) {
        List<Path> paths = new ArrayList<>();
        if (isWindows()) {
            addWindowsApplication(paths, "LOCALAPPDATA", applicationName);
            addWindowsApplication(paths, "PROGRAMFILES", applicationName);
            addWindowsApplication(paths, "PROGRAMFILES(X86)", applicationName);
        } else {
            paths.add(systemApplications.resolve(applicationName + ".app"));
            paths.add(home.resolve("Applications").resolve(applicationName + ".app"));
        }
        return paths;
    }

    private void addWindowsApplication(List<Path> paths, String environmentKey, String applicationName) {
        String root = environmentValue(environmentKey);
        if (root == null || root.isBlank()) return;
        Path base = Path.of(root);
        paths.add(base.resolve(applicationName).resolve(applicationName + ".exe"));
        paths.add(base.resolve("Programs").resolve(applicationName).resolve(applicationName + ".exe"));
    }

    private void addEnvironmentPath(Set<Path> paths, String environmentKey, String child) {
        String root = environmentValue(environmentKey);
        if (root != null && !root.isBlank()) paths.add(Path.of(root).resolve(child));
    }

    private String environmentValue(String key) {
        return environment.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    private boolean isWindows() {
        return osName.contains("win");
    }
}
