import 'dart:convert';
import 'dart:io';

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
      expect(request.headers['user-agent'], 'SkillPort-Flutter/1.0.31');
      return http.Response(
        jsonEncode(<String, dynamic>{
          'version': '1.0.18',
          'date': '2026-08-26',
          'title': '下一版本',
          'changes': <String>['新增自动更新'],
          'macosUrl':
              'https://www.jmuyuer.com/bridge/client/SkillPort-Bridge.pkg?v=1.0.18',
          'windowsUrl':
              'https://www.jmuyuer.com/bridge/client/SkillPort-Setup.exe?v=1.0.18',
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final service = ClientReleaseService(httpClient: client);

    final release = await service.fetchLatest();

    expect(release.version, '1.0.18');
    expect(release.changes, <String>['新增自动更新']);
    expect(isNewerVersion(release.version, '1.0.10'), isTrue);
  });

  test('rejects an update URL hosted outside SkillPort', () async {
    final client = MockClient(
      (request) async => http.Response(
        jsonEncode(<String, dynamic>{
          'version': '1.0.18',
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

  test('downloads the platform installer locally and launches it', () async {
    final payload = utf8.encode('skillport-installer-v1.0.18');
    late Directory temporaryDirectory;
    String? launchedOperatingSystem;
    String? launchedPath;
    final progress = <double>[];
    final client = MockClient((request) async {
      expect(
        request.url.toString(),
        'https://www.jmuyuer.com/bridge/client/SkillPort-Setup.exe?v=1.0.18',
      );
      expect(
        request.headers['user-agent'],
        'SkillPort-Flutter-Updater/1.0.31',
      );
      return http.Response.bytes(
        payload,
        200,
        headers: <String, String>{'content-length': '${payload.length}'},
      );
    });
    final service = ClientReleaseService(
      httpClient: client,
      operatingSystem: 'windows',
      temporaryDirectoryFactory: () async {
        temporaryDirectory = await Directory.systemTemp.createTemp(
          'skillport-updater-test-',
        );
        return temporaryDirectory;
      },
      installerLauncher: (operatingSystem, installerPath) async {
        launchedOperatingSystem = operatingSystem;
        launchedPath = installerPath;
      },
    );
    final release = ClientReleaseInfo(
      version: '1.0.18',
      date: '2026-08-25',
      title: '一键客户端自动更新',
      changes: <String>['自动更新'],
      macosUrl: Uri.parse(
        'https://www.jmuyuer.com/bridge/client/SkillPort-Bridge.pkg?v=1.0.18',
      ),
      windowsUrl: Uri.parse(
        'https://www.jmuyuer.com/bridge/client/SkillPort-Setup.exe?v=1.0.18',
      ),
    );

    final installer = await service.downloadAndLaunch(
      release,
      onProgress: progress.add,
    );

    expect(await installer.readAsBytes(), payload);
    expect(installer.path, endsWith('SkillPort-Setup.exe'));
    expect(launchedOperatingSystem, 'windows');
    expect(launchedPath, installer.path);
    expect(progress.first, 0);
    expect(progress.last, 1);
    await temporaryDirectory.delete(recursive: true);
  });
}
