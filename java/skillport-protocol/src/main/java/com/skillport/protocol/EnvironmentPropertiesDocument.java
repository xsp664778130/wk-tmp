package com.skillport.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class EnvironmentPropertiesDocument {
    public static final int MAX_PROPERTIES = 200;
    public static final int MAX_KEY_LENGTH = 128;
    public static final int MAX_VALUE_LENGTH = 4096;
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]{0,127}");

    private final List<String> lines;
    private final String lineSeparator;
    private final Map<String, PropertyLine> propertyLines;

    private EnvironmentPropertiesDocument(List<String> lines, String lineSeparator,
                                          Map<String, PropertyLine> propertyLines) {
        this.lines = List.copyOf(lines);
        this.lineSeparator = lineSeparator;
        this.propertyLines = Collections.unmodifiableMap(new LinkedHashMap<>(propertyLines));
    }

    public static EnvironmentPropertiesDocument parse(String content) {
        String normalized = content == null ? "" : content;
        String separator = normalized.contains("\r\n") ? "\r\n" : "\n";
        String[] sourceLines = normalized.split("\\r\\n|\\n|\\r", -1);
        List<String> lines = new ArrayList<>(List.of(sourceLines));
        Map<String, PropertyLine> properties = new LinkedHashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            PropertyLine property = parseLine(index, lines.get(index));
            if (property == null) continue;
            if (properties.putIfAbsent(property.key(), property) != null) {
                throw new IllegalArgumentException("env.properties 包含重复键：" + property.key());
            }
            if (properties.size() > MAX_PROPERTIES) {
                throw new IllegalArgumentException("env.properties 最多支持 " + MAX_PROPERTIES + " 个键");
            }
        }
        return new EnvironmentPropertiesDocument(lines, separator, properties);
    }

    public Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        propertyLines.forEach((key, property) -> values.put(key, property.value()));
        return Collections.unmodifiableMap(values);
    }

    public String updateValues(Map<String, String> updates) {
        if (updates == null) throw new IllegalArgumentException("env.properties 更新内容不能为空");
        for (String key : updates.keySet()) {
            validateKey(key);
            if (!propertyLines.containsKey(key)) {
                throw new IllegalArgumentException("env.properties 中不存在键：" + key);
            }
        }
        List<String> updatedLines = new ArrayList<>(lines);
        updates.forEach((key, value) -> {
            String normalizedValue = validateValue(value);
            PropertyLine property = propertyLines.get(key);
            updatedLines.set(property.index(), property.prefix() + normalizedValue);
        });
        return String.join(lineSeparator, updatedLines);
    }

    public static void validateUpdates(Map<String, String> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("请至少修改一个 env.properties 值");
        }
        if (updates.size() > MAX_PROPERTIES) {
            throw new IllegalArgumentException("env.properties 最多支持 " + MAX_PROPERTIES + " 个键");
        }
        updates.forEach((key, value) -> {
            validateKey(key);
            validateValue(value);
        });
    }

    private static PropertyLine parseLine(int index, String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) return null;
        int separatorIndex = separatorIndex(line);
        if (separatorIndex < 0) return null;
        String key = line.substring(0, separatorIndex).trim();
        validateKey(key);
        int valueStart = separatorIndex + 1;
        while (valueStart < line.length()) {
            char character = line.charAt(valueStart);
            if (character != ' ' && character != '\t') break;
            valueStart++;
        }
        String value = line.substring(valueStart);
        validateValue(value);
        return new PropertyLine(index, key, line.substring(0, valueStart), value);
    }

    private static int separatorIndex(String line) {
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (!escaped && (character == '=' || character == ':')) return index;
            if (character == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }
        }
        return -1;
    }

    private static void validateKey(String key) {
        if (key == null || key.length() > MAX_KEY_LENGTH || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("env.properties 键格式不正确");
        }
    }

    private static String validateValue(String value) {
        if (value == null) throw new IllegalArgumentException("env.properties 值不能为空对象");
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("env.properties 单个值不能超过 " + MAX_VALUE_LENGTH + " 个字符");
        }
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("env.properties 值不能包含换行符或空字符");
        }
        return value;
    }

    private record PropertyLine(int index, String key, String prefix, String value) {
    }
}
