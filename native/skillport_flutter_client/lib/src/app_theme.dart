import 'dart:io';

import 'package:flutter/material.dart';

enum SkillPortThemePreset { midnight, graphite, ocean, aurora, rose, daylight }

extension SkillPortThemePresetInfo on SkillPortThemePreset {
  String get label => switch (this) {
    SkillPortThemePreset.midnight => '深夜紫',
    SkillPortThemePreset.graphite => '曜石黑',
    SkillPortThemePreset.ocean => '海湾蓝',
    SkillPortThemePreset.aurora => '极光青',
    SkillPortThemePreset.rose => '暮霞玫',
    SkillPortThemePreset.daylight => '晨雾白',
  };

  String get description => switch (this) {
    SkillPortThemePreset.midnight => '紫蓝星云高光',
    SkillPortThemePreset.graphite => '暖金曜石高光',
    SkillPortThemePreset.ocean => '深海蓝青高光',
    SkillPortThemePreset.aurora => '青绿与紫光交叠',
    SkillPortThemePreset.rose => '玫红与暖橙霓虹',
    SkillPortThemePreset.daylight => '明亮柔和的浅色',
  };

  Color get previewColor => switch (this) {
    SkillPortThemePreset.midnight => const Color(0xFF8168FF),
    SkillPortThemePreset.graphite => const Color(0xFFD69A6A),
    SkillPortThemePreset.ocean => const Color(0xFF35B8D8),
    SkillPortThemePreset.aurora => const Color(0xFF38D6B5),
    SkillPortThemePreset.rose => const Color(0xFFF065A7),
    SkillPortThemePreset.daylight => const Color(0xFF7457E8),
  };

  List<Color> get previewColors => switch (this) {
    SkillPortThemePreset.midnight => const <Color>[Color(0xFF7457E8), Color(0xFF3B68FF)],
    SkillPortThemePreset.graphite => const <Color>[Color(0xFFD69A6A), Color(0xFF70533E)],
    SkillPortThemePreset.ocean => const <Color>[Color(0xFF35B8D8), Color(0xFF2464E8)],
    SkillPortThemePreset.aurora => const <Color>[Color(0xFF38D6B5), Color(0xFF826BFF)],
    SkillPortThemePreset.rose => const <Color>[Color(0xFFF065A7), Color(0xFFF29A62)],
    SkillPortThemePreset.daylight => const <Color>[Color(0xFFFFFFFF), Color(0xFFD9CFFF)],
  };
}

class _GlowSpot {
  const _GlowSpot(this.alignment, this.color, this.radius, this.opacity);

  final Alignment alignment;
  final Color color;
  final double radius;
  final double opacity;
}

extension _SkillPortThemeGlowInfo on SkillPortThemePreset {
  List<_GlowSpot> get glowSpots => switch (this) {
    SkillPortThemePreset.midnight => const <_GlowSpot>[
      _GlowSpot(Alignment(.48, -.72), Color(0xFF684BFF), .48, .23),
      _GlowSpot(Alignment(-.26, .95), Color(0xFF285BD8), .62, .16),
      _GlowSpot(Alignment(1.02, .28), Color(0xFF8D44E8), .42, .10),
    ],
    SkillPortThemePreset.graphite => const <_GlowSpot>[
      _GlowSpot(Alignment(.64, -.78), Color(0xFFE19A62), .48, .15),
      _GlowSpot(Alignment(-.72, .72), Color(0xFF9D5A38), .54, .10),
      _GlowSpot(Alignment(.88, .82), Color(0xFF6A4A35), .42, .08),
    ],
    SkillPortThemePreset.ocean => const <_GlowSpot>[
      _GlowSpot(Alignment(.58, -.78), Color(0xFF247CF2), .50, .19),
      _GlowSpot(Alignment(-.55, .72), Color(0xFF17B9D6), .58, .15),
      _GlowSpot(Alignment(1.08, .42), Color(0xFF31D3B2), .38, .09),
    ],
    SkillPortThemePreset.aurora => const <_GlowSpot>[
      _GlowSpot(Alignment(-.65, -.72), Color(0xFF18CFA8), .52, .18),
      _GlowSpot(Alignment(.62, -.48), Color(0xFF7457E8), .52, .20),
      _GlowSpot(Alignment(.12, 1.04), Color(0xFF1A84D9), .62, .14),
    ],
    SkillPortThemePreset.rose => const <_GlowSpot>[
      _GlowSpot(Alignment(.66, -.72), Color(0xFFE94196), .50, .20),
      _GlowSpot(Alignment(-.72, .60), Color(0xFFF28B54), .58, .13),
      _GlowSpot(Alignment(.68, .92), Color(0xFF7F55EA), .46, .10),
    ],
    SkillPortThemePreset.daylight => const <_GlowSpot>[
      _GlowSpot(Alignment(.72, -.72), Color(0xFFA993FF), .48, .11),
      _GlowSpot(Alignment(-.70, .82), Color(0xFF94DCEC), .56, .09),
    ],
  };
}

class ThemeGlowBackdrop extends StatelessWidget {
  const ThemeGlowBackdrop({super.key, required this.preset});

  final SkillPortThemePreset preset;

  @override
  Widget build(BuildContext context) => Positioned.fill(
    child: IgnorePointer(
      child: AnimatedSwitcher(
        duration: const Duration(milliseconds: 420),
        switchInCurve: Curves.easeOutCubic,
        switchOutCurve: Curves.easeInCubic,
        child: RepaintBoundary(
          key: ValueKey<SkillPortThemePreset>(preset),
          child: CustomPaint(painter: _ThemeGlowPainter(preset.glowSpots)),
        ),
      ),
    ),
  );
}

class ThemePresetSwatch extends StatelessWidget {
  const ThemePresetSwatch({super.key, required this.preset, this.size = 26});

  final SkillPortThemePreset preset;
  final double size;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      gradient: LinearGradient(
        colors: preset.previewColors,
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
      ),
      border: Border.all(color: Colors.white.withValues(alpha: .25)),
      boxShadow: <BoxShadow>[
        BoxShadow(
          color: preset.previewColor.withValues(alpha: .42),
          blurRadius: 10,
          spreadRadius: 1,
        ),
      ],
    ),
  );
}

class _ThemeGlowPainter extends CustomPainter {
  const _ThemeGlowPainter(this.spots);

  final List<_GlowSpot> spots;

  @override
  void paint(Canvas canvas, Size size) {
    final shortestSide = size.shortestSide;
    for (final spot in spots) {
      final center = Offset(
        (spot.alignment.x + 1) * size.width / 2,
        (spot.alignment.y + 1) * size.height / 2,
      );
      final radius = shortestSide * spot.radius;
      final paint = Paint()
        ..shader = RadialGradient(
          colors: <Color>[
            spot.color.withValues(alpha: spot.opacity),
            spot.color.withValues(alpha: spot.opacity * .34),
            Colors.transparent,
          ],
          stops: const <double>[0, .42, 1],
        ).createShader(Rect.fromCircle(center: center, radius: radius));
      canvas.drawCircle(center, radius, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _ThemeGlowPainter oldDelegate) =>
      oldDelegate.spots != spots;
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
    SkillPortThemePreset.aurora => (
      seed: const Color(0xFF38D6B5),
      canvas: const Color(0xFF071217),
      surface: const Color(0xFF101C22),
      sidebar: const Color(0xFF09151A),
      rail: const Color(0xFF081419),
      field: const Color(0xFF0D191F),
      soft: const Color(0xFF173038),
      line: const Color(0xFF254047),
      ink: const Color(0xFFF0FBF9),
      muted: const Color(0xFF91AAA9),
    ),
    SkillPortThemePreset.rose => (
      seed: const Color(0xFFF065A7),
      canvas: const Color(0xFF140B13),
      surface: const Color(0xFF21131E),
      sidebar: const Color(0xFF170E16),
      rail: const Color(0xFF160D15),
      field: const Color(0xFF1D111B),
      soft: const Color(0xFF34202F),
      line: const Color(0xFF43283B),
      ink: const Color(0xFFFFF1F8),
      muted: const Color(0xFFB59CAA),
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
