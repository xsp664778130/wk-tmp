import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:skillport_client/src/api_client.dart';
import 'package:skillport_client/src/models.dart';

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

  test('updates the account profile and sends password recovery requests', () async {
    var requestNumber = 0;
    final client = MockClient((request) async {
      requestNumber += 1;
      if (requestNumber == 1) {
        expect(request.method, 'PATCH');
        expect(request.url.path, '/api/auth/profile');
        expect(request.headers['cookie'], 'skillport_session=token-123');
        expect(jsonDecode(request.body), <String, String>{'displayName': 'New Name'});
        return http.Response(
          jsonEncode(<String, dynamic>{
            'id': 'u1',
            'email': 'user@example.com',
            'displayName': 'New Name',
            'passwordEnabled': true,
          }),
          200,
          headers: const <String, String>{'content-type': 'application/json'},
        );
      }
      expect(request.method, 'POST');
      expect(request.url.path, '/api/auth/password/reset-code');
      expect(jsonDecode(request.body), <String, String>{'email': 'user@example.com'});
      return http.Response('', 202);
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';

    final profile = await api.updateProfile('New Name');
    expect(profile.displayName, 'New Name');
    expect(profile.passwordEnabled, isTrue);
    await api.requestPasswordResetCode('user@example.com');
  });

  test('updates a private Skill category through the synchronized API', () async {
    final client = MockClient((request) async {
      expect(request.method, 'PATCH');
      expect(request.url.path, '/api/skills/s1');
      expect(request.headers['cookie'], 'skillport_session=token-123');
      expect(jsonDecode(request.body), <String, String>{'category': '日志技能'});
      return http.Response(
        jsonEncode(<String, dynamic>{
          'id': 's1',
          'name': 'Audit Skill',
          'description': 'Checks infrastructure',
          'category': '日志技能',
          'fileName': 'audit.zip',
          'sizeBytes': 12,
          'sha256': 'abc',
          'toolCompatibility': 'codex,qoder',
          'shared': true,
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';

    final skill = await api.updateCategory('s1', '日志技能');

    expect(skill.category, '日志技能');
    expect(skill.shared, isTrue);
  });

  test('reads public env.properties as read-only and updates private values', () async {
    var requestNumber = 0;
    final client = MockClient((request) async {
      requestNumber += 1;
      if (requestNumber == 1) {
        expect(request.method, 'GET');
        expect(request.url.path, '/api/public-skills/public-1/environment');
        return http.Response(
          jsonEncode(<String, dynamic>{
            'exists': true,
            'path': 'demo/env.properties',
            'values': <String, String>{'API_URL': 'https://example.com'},
            'editable': false,
          }),
          200,
          headers: const <String, String>{'content-type': 'application/json'},
        );
      }
      expect(request.method, 'PATCH');
      expect(request.url.path, '/api/skills/private-1/environment');
      expect(jsonDecode(request.body), <String, dynamic>{
        'values': <String, String>{'API_URL': 'https://new.example.com'},
      });
      return http.Response(
        jsonEncode(<String, dynamic>{
          'exists': true,
          'path': 'demo/env.properties',
          'values': <String, String>{'API_URL': 'https://new.example.com'},
          'editable': true,
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';
    const publicSkill = SkillItem(
      id: 'public-1',
      name: 'Public',
      description: 'public',
      category: '编程技能',
      fileName: 'public.zip',
      sizeBytes: 1,
      sha256: 'abc',
      compatible: <String>['codex'],
      isPublic: true,
    );

    final publicEnvironment = await api.skillEnvironment(publicSkill);
    final privateEnvironment = await api.updateSkillEnvironment(
      'private-1',
      const <String, String>{'API_URL': 'https://new.example.com'},
    );

    expect(publicEnvironment.editable, isFalse);
    expect(publicEnvironment.values['API_URL'], 'https://example.com');
    expect(privateEnvironment.editable, isTrue);
    expect(privateEnvironment.values['API_URL'], 'https://new.example.com');
  });

  test('updates Skill details and usage steps through the synchronized API', () async {
    final client = MockClient((request) async {
      expect(request.method, 'PATCH');
      expect(request.url.path, '/api/skills/s1');
      final body = jsonDecode(request.body) as Map<String, dynamic>;
      expect(body['detail'], '完整排查数据库连接和慢查询。');
      expect(body['usageSteps'], <String>['选择数据库', '运行检查', '查看报告']);
      return http.Response(
        jsonEncode(<String, dynamic>{
          'id': 's1',
          'name': 'Audit Skill Pro',
          'description': 'Checks infrastructure',
          'detail': '完整排查数据库连接和慢查询。',
          'usageSteps': <String>['选择数据库', '运行检查', '查看报告'],
          'category': '排查技能',
          'fileName': 'audit.zip',
          'sizeBytes': 12,
          'sha256': 'abc',
          'toolCompatibility': 'codex,qoder',
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';

    final skill = await api.updateDetails(
      's1',
      name: 'Audit Skill Pro',
      description: 'Checks infrastructure',
      detail: '完整排查数据库连接和慢查询。',
      usageSteps: const <String>['选择数据库', '运行检查', '查看报告'],
    );

    expect(skill.name, 'Audit Skill Pro');
    expect(skill.usageSteps, hasLength(3));
  });

  test('replaces only the private Skill package through multipart PUT', () async {
    final directory = await Directory.systemTemp.createTemp('skillport-replace-test-');
    final file = File('${directory.path}/audit-v2.zip');
    await file.writeAsBytes(<int>[1, 2, 3, 4]);
    addTearDown(() => directory.delete(recursive: true));
    final client = MockClient((request) async {
      expect(request.method, 'PUT');
      expect(request.url.path, '/api/skills/s1/file');
      expect(request.headers['cookie'], 'skillport_session=token-123');
      expect(request.headers['content-type'], startsWith('multipart/form-data; boundary='));
      final uploadedBody = utf8.decode(request.bodyBytes);
      expect(uploadedBody, contains('name="file"'));
      expect(uploadedBody, contains('filename="audit-v2.zip"'));
      return http.Response(
        jsonEncode(<String, dynamic>{
          'id': 's1',
          'name': 'Audit Skill',
          'description': 'Metadata stays unchanged',
          'detail': 'Stable detail',
          'usageSteps': <String>['Run audit'],
          'category': '排查技能',
          'fileName': 'audit-v2.zip',
          'sizeBytes': 4,
          'sha256': 'new-hash',
          'note': 'private note',
          'toolCompatibility': 'codex,qoder',
          'shared': true,
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';

    final skill = await api.replaceSkillPackage('s1', file.path);

    expect(skill.fileName, 'audit-v2.zip');
    expect(skill.name, 'Audit Skill');
    expect(skill.note, 'private note');
    expect(skill.shared, isTrue);
  });

  test('deletes an owned Skill from the private workspace API', () async {
    final client = MockClient((request) async {
      expect(request.method, 'DELETE');
      expect(request.url.path, '/api/skills/s1');
      expect(request.headers['cookie'], 'skillport_session=token-123');
      return http.Response('', 204);
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';

    await api.deleteSkill('s1');
  });

  test('submits an authenticated opinion to the public mailbox API', () async {
    final client = MockClient((request) async {
      expect(request.method, 'POST');
      expect(request.url.path, '/api/feedback');
      expect(request.headers['cookie'], 'skillport_session=token-123');
      expect(jsonDecode(request.body), <String, String>{
        'kind': '功能建议',
        'content': '希望支持批量安装 Skill',
      });
      return http.Response(
        jsonEncode(<String, dynamic>{
          'id': 'feedback-1',
          'kind': '功能建议',
          'status': 'NEW',
          'createdAt': '2026-08-25T03:00:00Z',
        }),
        201,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client)..token = 'token-123';

    await api.submitFeedback(kind: '功能建议', content: '希望支持批量安装 Skill');
  });

  test('loads public feedback with submitter time and server pagination', () async {
    final client = MockClient((request) async {
      expect(request.method, 'GET');
      expect(request.url.path, '/api/feedback');
      expect(request.url.queryParameters, <String, String>{'page': '2', 'size': '6'});
      return http.Response(
        jsonEncode(<String, dynamic>{
          'items': <Map<String, dynamic>>[
            <String, dynamic>{
              'id': 'feedback-2',
              'submitter': '小明',
              'kind': '体验优化',
              'content': '希望公开意见支持分页',
              'createdAt': '2026-08-25T08:30:00Z',
            },
          ],
          'page': 2,
          'size': 6,
          'totalElements': 13,
          'totalPages': 3,
          'hasPrevious': true,
          'hasNext': true,
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final api = SkillPortApi(httpClient: client);

    final page = await api.feedbackPage(page: 2);

    expect(page.items.single.submitter, '小明');
    expect(page.items.single.createdAt.toUtc(), DateTime.parse('2026-08-25T08:30:00Z'));
    expect(page.page, 2);
    expect(page.totalPages, 3);
    expect(page.hasNext, isTrue);
  });
}
