import 'package:flutter_test/flutter_test.dart';
import 'package:skillport_client/src/models.dart';

void main() {
  test('uses the current tool set when compatibility is missing', () {
    final skill = SkillItem.fromPrivateJson(const <String, dynamic>{});

    expect(skill.compatible, <String>[
      'codex',
      'qoder',
      'opencode',
      'claude',
      'cursor',
    ]);
  });

  test('migrates the retired OpenAI target in legacy responses', () {
    final skill = SkillItem.fromPrivateJson(const <String, dynamic>{
      'toolCompatibility': 'codex,qoder,openai',
    });

    expect(skill.compatible, <String>['codex', 'qoder', 'opencode', 'claude']);
  });
}
