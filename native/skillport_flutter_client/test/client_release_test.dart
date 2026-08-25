import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:skillport_client/src/client_release.dart';

void main() {
  test('compares semantic client versions numerically', () {
    expect(isNewerVersion('1.0.10', '1.0.9'), isTrue);
    expect(isNewerVersion('v2.0.0', '1.99.99'), isTrue);
    expect(isNewerVersion('1.0.10', '1.0.10'), isFalse);
    expect(isNewerVersion('1.0.9', '1.0.10'), isFalse);
    expect(isNewerVersion('not-a-version', '1.0.10'), isFalse);
  });

  test('loads the public latest-client manifest', () async {
    final client = MockClient((request) async {
      expect(request.url.path, '/bridge/client/latest.json');
      expect(request.headers['user-agent'], 'SkillPort-Flutter/1.0.14');
      return http.Response(
        jsonEncode(<String, dynamic>{
          'version': '1.0.14',
          'date': '2026-08-26',
          'title': '下一版本',
          'changes': <String>['新增自动更新'],
          'macosUrl':
              'https://www.jmuyuer.com/bridge/client/SkillPort-Bridge.pkg?v=1.0.14',
          'windowsUrl':
              'https://www.jmuyuer.com/bridge/client/SkillPort-Setup.exe?v=1.0.14',
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final service = ClientReleaseService(httpClient: client);

    final release = await service.fetchLatest();

    expect(release.version, '1.0.14');
    expect(release.changes, <String>['新增自动更新']);
    expect(isNewerVersion(release.version, '1.0.10'), isTrue);
  });

  test('rejects an update URL hosted outside SkillPort', () async {
    final client = MockClient(
      (request) async => http.Response(
        jsonEncode(<String, dynamic>{
          'version': '1.0.14',
          'date': '2026-08-26',
          'title': '不安全更新',
          'changes': <String>[],
          'macosUrl': 'https://example.com/update.pkg',
          'windowsUrl': 'https://example.com/update.exe',
        }),
        200,
        headers: const <String, String>{
          'content-type': 'application/json; charset=utf-8',
        },
      ),
    );
    final service = ClientReleaseService(httpClient: client);

    expect(service.fetchLatest(), throwsA(isA<FormatException>()));
  });
}
