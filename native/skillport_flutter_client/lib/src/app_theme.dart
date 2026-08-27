import 'dart:io';

import 'package:flutter/material.dart';

enum SkillPortThemePreset { midnight, graphite, ocean, daylight }

extension SkillPortThemePresetInfo on SkillPortThemePreset {
  String get label => switch (this) {
    SkillPortThemePreset.midnight => '深夜紫',
    SkillPortThemePreset.graphite => '曜石黑',
    SkillPortThemePreset.ocean => '海湾蓝',
    SkillPortThemePreset.daylight => '晨雾白',
  };

  String get description => switch (this) {
    SkillPortThemePreset.midnight => '参考图同款暗色',
    SkillPortThemePreset.graphite => '中性克制的黑灰',
    SkillPortThemePreset.ocean => '沉静的蓝青配色',
    SkillPortThemePreset.daylight => '明亮柔和的浅色',
  };

  Color get previewColor => switch (this) {
    SkillPortThemePreset.midnight => const Color(0xFF8168FF),
    SkillPortThemePreset.graphite => const Color(0xFFD69A6A),
    SkillPortThemePreset.ocean => const Color(0xFF35B8D8),
    SkillPortThemePreset.daylight => const Color(0xFF7457E8),
  };
}

class SkillPortPalette extends ThemeExtension<SkillPortPalette> {
  const SkillPortPalette({
    required this.sidebar,
    required this.rail,
    required this.field,
    required this.soft,
    required this.successSurface,
    required this.success,
  });

  final Color sidebar;
  final Color rail;
  final Color field;
  final Color soft;
  final Color successSurface;
  final Color success;

  @override
  SkillPortPalette copyWith({
    Color? sidebar,
    Color? rail,
    Color? field,
    Color? soft,
    Color? successSurface,
    Color? success,
  }) => SkillPortPalette(
    sidebar: sidebar ?? this.sidebar,
    rail: rail ?? this.rail,
    field: field ?? this.field,
    soft: soft ?? this.soft,
    successSurface: successSurface ?? this.successSurface,
    success: success ?? this.success,
  );

  @override
  SkillPortPalette lerp(covariant SkillPortPalette? other, double t) {
    if (other == null) return this;
    return SkillPortPalette(
      sidebar: Color.lerp(sidebar, other.sidebar, t)!,
      rail: Color.lerp(rail, other.rail, t)!,
      field: Color.lerp(field, other.field, t)!,
      soft: Color.lerp(soft, other.soft, t)!,
      successSurface: Color.lerp(successSurface, other.successSurface, t)!,
      success: Color.lerp(success, other.success, t)!,
    );
  }
}

ThemeData buildSkillPortTheme(SkillPortThemePreset preset) {
  final dark = preset != SkillPortThemePreset.daylight;
  final colors = switch (preset) {
    SkillPortThemePreset.midnight => (
      seed: const Color(0xFF8168FF),
      canvas: const Color(0xFF090C13),
      surface: const Color(0xFF121620),
      sidebar: const Color(0xFF0C1018),
      rail: const Color(0xFF0B0F17),
      field: const Color(0xFF10141E),
      soft: const Color(0xFF1C2030),
      line: const Color(0xFF272C3A),
      ink: const Color(0xFFF4F1FA),
      muted: const Color(0xFF9B98AA),
    ),
    SkillPortThemePreset.graphite => (
      seed: const Color(0xFFD69A6A),
      canvas: const Color(0xFF141517),
      surface: const Color(0xFF1F2022),
      sidebar: const Color(0xFF191A1C),
      rail: const Color(0xFF18191B),
      field: const Color(0xFF1C1D1F),
      soft: const Color(0xFF2B2C2F),
      line: const Color(0xFF343537),
      ink: const Color(0xFFF1F1EF),
      muted: const Color(0xFFA2A2A0),
    ),
    SkillPortThemePreset.ocean => (
      seed: const Color(0xFF35B8D8),
      canvas: const Color(0xFF07141C),
      surface: const Color(0xFF0E2028),
      sidebar: const Color(0xFF091921),
      rail: const Color(0xFF08171F),
      field: const Color(0xFF0B1B23),
      soft: const Color(0xFF16313B),
      line: const Color(0xFF1E3943),
      ink: const Color(0xFFEDF8FB),
      muted: const Color(0xFF8EA8B1),
    ),
    SkillPortThemePreset.daylight => (
      seed: const Color(0xFF7457E8),
      canvas: const Color(0xFFFAF9F6),
      surface: Colors.white,
      sidebar: const Color(0xFFF0ECFA),
      rail: const Color(0xFFFBFAF7),
      field: const Color(0xFFF7F5F1),
      soft: const Color(0xFFF2F0EC),
      line: const Color(0xFFE6E2DB),
      ink: const Color(0xFF29272E),
      muted: const Color(0xFF77737C),
    ),
  };
  final scheme = ColorScheme.fromSeed(
    seedColor: colors.seed,
    brightness: dark ? Brightness.dark : Brightness.light,
  ).copyWith(
    primary: colors.seed,
    surface: colors.surface,
    onSurface: colors.ink,
    onSurfaceVariant: colors.muted,
    outlineVariant: colors.line,
  );
  final radius = BorderRadius.circular(12);
  return ThemeData(
    useMaterial3: true,
    brightness: dark ? Brightness.dark : Brightness.light,
    colorScheme: scheme,
    scaffoldBackgroundColor: colors.canvas,
    fontFamily: Platform.isWindows ? 'Microsoft YaHei UI' : '.AppleSystemUIFont',
    cardTheme: CardThemeData(color: colors.surface, elevation: 0),
    dialogTheme: DialogThemeData(backgroundColor: colors.surface),
    dividerColor: colors.line,
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: colors.field,
      border: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: colors.line)),
      enabledBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: colors.line)),
      focusedBorder: OutlineInputBorder(borderRadius: radius, borderSide: BorderSide(color: colors.seed, width: 1.4)),
    ),
    extensions: <ThemeExtension<dynamic>>[
      SkillPortPalette(
        sidebar: colors.sidebar,
        rail: colors.rail,
        field: colors.field,
        soft: colors.soft,
        successSurface: dark ? const Color(0xFF1A3224) : const Color(0xFFE6F8CE),
        success: dark ? const Color(0xFF91D66B) : const Color(0xFF4F7E2B),
      ),
    ],
  );
}

SkillPortPalette skillPortPalette(BuildContext context) =>
    Theme.of(context).extension<SkillPortPalette>()!;

class ThemePreferenceStore {
  static const _fileName = 'theme.preference';

  static Directory? _supportDirectory() {
    final environment = Platform.environment;
    if (Platform.isMacOS) {
      final home = environment['HOME'];
      return home == null ? null : Directory('$home/Library/Application Support/SkillPort');
    }
    if (Platform.isWindows) {
      final appData = environment['APPDATA'];
      return appData == null ? null : Directory('$appData\\SkillPort');
    }
    final home = environment['HOME'];
    return home == null ? null : Directory('$home/.config/skillport');
  }

  static Future<SkillPortThemePreset> read() async {
    try {
      final directory = _supportDirectory();
      if (directory == null) return SkillPortThemePreset.midnight;
      final file = File('${directory.path}${Platform.pathSeparator}$_fileName');
      if (!await file.exists()) return SkillPortThemePreset.midnight;
      final value = (await file.readAsString()).trim();
      return SkillPortThemePreset.values.firstWhere(
        (preset) => preset.name == value,
        orElse: () => SkillPortThemePreset.midnight,
      );
    } catch (_) {
      return SkillPortThemePreset.midnight;
    }
  }

  static Future<void> write(SkillPortThemePreset preset) async {
    try {
      final directory = _supportDirectory();
      if (directory == null) return;
      await directory.create(recursive: true);
      await File('${directory.path}${Platform.pathSeparator}$_fileName').writeAsString(preset.name, flush: true);
    } catch (_) {
      // Theme persistence is best-effort and must never block the client.
    }
  }
}
