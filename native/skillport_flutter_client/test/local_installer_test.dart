import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:crypto/crypto.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as path;
import 'package:skillport_client/src/local_installer.dart';
import 'package:skillport_client/src/models.dart';

void main() {
  group('LocalInstaller', () {
    late Directory home;
    late LocalInstaller installer;

    setUp(() async {
      home = await Directory.systemTemp.createTemp('skillport-flutter-test-');
      installer = LocalInstaller(
        homeDirectory: home.path,
        environment: const <String, String>{},
        isMacOS: false,
        isWindows: false,
      );
      await Directory(path.join(home.path, '.codex')).create();
    });

    tearDown(() async {
      if (home.existsSync()) await home.delete(recursive: true);
    });

    test('exposes every supported tool and its official Skill directory', () {
      final tools = installer.detectTools();

      expect(tools.map((tool) => tool.id), <String>[
        'codex',
        'qoder',
        'opencode',
        'claude',
        'cursor',
      ]);
      expect(
        tools.map((tool) => path.relative(tool.directory, from: home.path)),
        <String>[
          path.join('.codex', 'skills'),
          path.join('.qoder', 'skills'),
          path.join('.config', 'opencode', 'skills'),
          path.join('.claude', 'skills'),
          path.join('.cursor', 'skills'),
        ],
      );
      expect(
        tools.where((tool) => tool.detected).map((tool) => tool.id),
        isEmpty,
      );
    });

    test('detects all supported command-line tools from PATH', () async {
      final binaries = await Directory(path.join(home.path, 'bin')).create();
      final suffix = Platform.isWindows ? '.cmd' : '';
      for (final command in <String>[
        'codex',
        'qoder',
        'opencode',
        'claude',
        'cursor',
      ]) {
        await File(path.join(binaries.path, '$command$suffix'))
            .writeAsString('');
      }
      final pathInstaller = LocalInstaller(
        homeDirectory: home.path,
        environment: <String, String>{'PATH': binaries.path},
        isMacOS: false,
        isWindows: Platform.isWindows,
      );

      expect(
        pathInstaller
            .detectTools()
            .where((tool) => tool.detected)
            .map((tool) => tool.id),
        <String>['codex', 'qoder', 'opencode', 'claude', 'cursor'],
      );
    });

    test(
      'installs a rooted ZIP, verifies SHA-256, and uninstalls without backup',
      () async {
        final archive = Archive()
          ..addFile(
            ArchiveFile.string(
              'sample-skill/SKILL.md',
              '---\nname: sample-skill\ndescription: test\n---\n',
            ),
          )
          ..addFile(
            ArchiveFile.string(
              'sample-skill/scripts/check.sh',
              '#!/bin/sh\necho ok\n',
            )..mode = 0x1ed,
          );
        final content = Uint8List.fromList(ZipEncoder().encode(archive));
        final skill = _skill(sha256.convert(content).toString());

        await installer.install(
          skill: skill,
          content: content,
          targets: const <String>['codex', 'cursor'],
        );

        final installed = Directory(
          path.join(home.path, '.codex', 'skills', 'sample-skill'),
        );
        expect(
          File(path.join(installed.path, 'SKILL.md')).existsSync(),
          isTrue,
        );
        expect(
          File(path.join(installed.path, 'scripts', 'check.sh')).existsSync(),
          isTrue,
        );
        expect(installer.isInstalled(skill, 'codex'), isTrue);
        final cursorInstalled = Directory(
          path.join(home.path, '.cursor', 'skills', 'sample-skill'),
        );
        expect(
          File(path.join(cursorInstalled.path, 'SKILL.md')).existsSync(),
          isTrue,
        );
        expect(installer.isInstalled(skill, 'cursor'), isTrue);

        final removed = await installer.uninstall(
          skill: skill,
          targets: const <String>['codex', 'cursor'],
        );
        expect(removed, 2);
        expect(installed.existsSync(), isFalse);
        expect(cursorInstalled.existsSync(), isFalse);
        expect(
          Directory(path.join(home.path, '.codex', 'skills'))
              .listSync()
              .whereType<Directory>(),
          isEmpty,
        );
        expect(
          Directory(path.join(home.path, '.cursor', 'skills'))
              .listSync()
              .whereType<Directory>(),
          isEmpty,
        );
      },
    );

    test(
      'rejects an archive traversal path before writing outside destination',
      () async {
        final archive = Archive()
          ..addFile(ArchiveFile.string('../escape.txt', 'blocked'))
          ..addFile(
            ArchiveFile.string(
              'SKILL.md',
              '---\nname: bad\ndescription: bad\n---\n',
            ),
          );
        final content = Uint8List.fromList(ZipEncoder().encode(archive));

        await expectLater(
          installer.install(
            skill: _skill(sha256.convert(content).toString()),
            content: content,
            targets: const <String>['codex'],
          ),
          throwsA(isA<LocalInstallException>()),
        );
        expect(File(path.join(home.path, 'escape.txt')).existsSync(), isFalse);
      },
    );

    test('rejects content whose SHA-256 does not match', () async {
      final content = Uint8List.fromList(utf8.encode('not the expected file'));
      await expectLater(
        installer.install(
          skill: _skill(List<String>.filled(64, '0').join()),
          content: content,
          targets: const <String>['codex'],
        ),
        throwsA(isA<LocalInstallException>()),
      );
    });
  });

  test('skillSlug keeps Unicode letters and normalizes separators', () {
    expect(skillSlug('  Infrastructure Audit 技能  '), 'infrastructure-audit-技能');
    expect(skillSlug('***'), 'skillport-skill');
  });
}

SkillItem _skill(String digest) => SkillItem(
  id: 'skill-1',
  name: 'Sample Skill',
  description: 'test',
  category: '测试技能',
  fileName: 'sample.zip',
  sizeBytes: 0,
  sha256: digest,
  compatible: const <String>['codex'],
);
