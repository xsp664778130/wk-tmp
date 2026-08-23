import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:skillport_client/src/api_client.dart';

void main() {
  test('logs in through the native cookie-session API', () async {
    final client = MockClient((request) async {
      expect(request.url.path, '/api/auth/login');
      expect(request.headers['content-type'], contains('application/json'));
      final body = jsonDecode(request.body) as Map<String, dynamic>;
      expect(body['email'], 'user@example.com');
      return http.Response(
        jsonEncode(<String, dynamic>{
          'user': <String, String>{
            'id': 'u1',
            'email': 'user@example.com',
            'displayName': 'Tester',
          },
        }),
        200,
        headers: const <String, String>{
          'content-type': 'application/json',
          'set-cookie': 'skillport_session=native-session-token; Path=/; Secure; HttpOnly; SameSite=Lax',
        },
      );
    });
    final api = SkillPortApi(httpClient: client);
    final result = await api.login('user@example.com', 'password123');
    expect(result.token, 'native-session-token');
    expect(result.user.displayName, 'Tester');
  });

  test(
    'sends the stored secure session cookie when loading private skills',
    () async {
      final client = MockClient((request) async {
        expect(request.url.path, '/api/skills');
        expect(request.headers['cookie'], 'skillport_session=token-123');
        return http.Response(
          jsonEncode(<String, dynamic>{
            'skills': <Map<String, dynamic>>[
              <String, dynamic>{
                'id': 's1',
                'name': 'Audit Skill',
                'description': 'Checks infrastructure',
                'category': '排查工具',
                'fileName': 'audit.zip',
                'sizeBytes': 12,
                'sha256': 'abc',
                'note': 'private',
                'toolCompatibility': 'codex,qoder',
              },
            ],
          }),
          200,
          headers: const <String, String>{'content-type': 'application/json'},
        );
      });
      final api = SkillPortApi(httpClient: client)..token = 'token-123';
      final skills = await api.privateSkills();
      expect(skills.single.category, '排查技能');
      expect(skills.single.note, 'private');
    },
  );

  test('updates a private note through the browser-safe API', () async {
    final client = MockClient((request) async {
      expect(request.method, 'PATCH');
      expect(request.url.path, '/api/skills');
      expect(jsonDecode(request.body), <String, String>{
        'id': 's1',
        'note': '分享时也要带上',
      });
      return http.Response(
        jsonEncode(<String, dynamic>{
          'id': 's1',
          'name': 'Audit Skill',
          'description': 'Checks infrastructure',
          'category': '排查技能',
          'fileName': 'audit.zip',
          'sizeBytes': 12,
          'sha256': 'abc',
          'note': '分享时也要带上',
          'toolCompatibility': 'codex,qoder',
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';
    final skill = await api.updateNote('s1', '分享时也要带上');
    expect(skill.note, '分享时也要带上');
  });
}
