import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:skillport_client/src/app_theme.dart';

void main() {
  test('all theme presets provide a complete theme and preview', () {
    expect(SkillPortThemePreset.values, hasLength(6));

    for (final preset in SkillPortThemePreset.values) {
      final theme = buildSkillPortTheme(preset);
      expect(preset.label, isNotEmpty);
      expect(preset.description, isNotEmpty);
      expect(preset.previewColors, hasLength(2));
      expect(theme.extension<SkillPortPalette>(), isNotNull);
    }
  });

  testWidgets('glow backdrop and preset swatch render for every theme', (
    tester,
  ) async {
    for (final preset in SkillPortThemePreset.values) {
      await tester.pumpWidget(
        MaterialApp(
          theme: buildSkillPortTheme(preset),
          home: Scaffold(
            body: Stack(
              children: <Widget>[
                ThemeGlowBackdrop(preset: preset),
                Center(child: ThemePresetSwatch(preset: preset)),
              ],
            ),
          ),
        ),
      );
      expect(find.byType(CustomPaint), findsWidgets);
      expect(find.byType(ThemePresetSwatch), findsOneWidget);
    }
  });
}
