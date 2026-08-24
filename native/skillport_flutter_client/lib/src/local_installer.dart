import 'dart:io';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:crypto/crypto.dart';
import 'package:path/path.dart' as path;

import 'models.dart';

const toolDirectories = <String, String>{
  'codex': '.codex/skills',
  'qoder': '.qoder/skills',
  'opencode': '.config/opencode/skills',
  'claude': '.claude/skills',
};

const toolLabels = <String, String>{
  'codex': 'Codex',
  'qoder': 'Qoder',
  'opencode': 'OpenCode',
  'claude': 'Claude Code',
};

const toolMarks = <String, String>{
  'codex': 'CX',
  'qoder': 'Q',
  'opencode': 'OC',
  'claude': 'CC',
};

class LocalInstaller {
  LocalInstaller({
    String? homeDirectory,
    Map<String, String>? environment,
    bool? isMacOS,
    bool? isWindows,
  }) : _home = homeDirectory ?? resolveHomeDirectory(),
       _environment = environment ?? Platform.environment,
       _isMacOS = isMacOS ?? Platform.isMacOS,
       _isWindows = isWindows ?? Platform.isWindows;

  final String _home;
  final Map<String, String> _environment;
  final bool _isMacOS;
  final bool _isWindows;

  static String resolveHomeDirectory() {
    final value = Platform.isWindows
        ? Platform.environment['USERPROFILE']
        : Platform.environment['HOME'];
    if (value == null || value.trim().isEmpty) {
      throw const LocalInstallException('无法读取当前用户目录');
    }
    return path.normalize(path.absolute(value));
  }

  List<ToolTarget> detectTools() => toolDirectories.entries.map((entry) {
    final directory = path.joinAll(<String>[_home, ...entry.value.split('/')]);
    return ToolTarget(
      id: entry.key,
      name: toolLabels[entry.key]!,
      directory: directory,
      detected: _isToolInstalled(entry.key),
    );
  }).toList();

  bool _isToolInstalled(String toolId) {
    if (_executableExists(toolId)) return true;
    switch (toolId) {
      case 'codex':
        return _exists('.codex/config.toml') ||
            _exists('.codex/auth.json') ||
            _macAppExists('Codex');
      case 'qoder':
        return _macAppExists('Qoder') ||
            _macAppExists('Qoder IDE') ||
            _macAppExists('Qoder CN') ||
            _windowsQoderExists();
      case 'opencode':
        return _exists('.opencode/bin/opencode') ||
            _exists('.config/opencode/opencode.json') ||
            _exists('.config/opencode/opencode.jsonc') ||
            _macAppExists('OpenCode');
      case 'claude':
        return _exists('.claude/local/claude') ||
            _exists('.claude.json') ||
            _exists('.claude/settings.json');
      default:
        return false;
    }
  }

  bool _exists(String relative) =>
      File(path.joinAll(<String>[_home, ...relative.split('/')])).existsSync();

  bool _macAppExists(String name) =>
      _isMacOS &&
      <String>[
        path.join('/Applications', '$name.app'),
        path.join(_home, 'Applications', '$name.app'),
      ].any((candidate) => Directory(candidate).existsSync());

  bool _windowsQoderExists() {
    if (!_isWindows) return false;
    final roots = <String?>[
      _environment['LOCALAPPDATA'],
      _environment['PROGRAMFILES'],
      _environment['PROGRAMFILES(X86)'],
    ].whereType<String>();
    const names = <String>['Qoder', 'Qoder IDE', 'Qoder CN'];
    return roots.any(
      (root) => names.any(
        (name) => <String>[
          path.join(root, name, '$name.exe'),
          path.join(root, 'Programs', name, '$name.exe'),
          path.join(root, 'Programs', name, 'Qoder.exe'),
        ].any((candidate) => File(candidate).existsSync()),
      ),
    );
  }

  bool _executableExists(String command) {
    final pathValue = _environment['PATH'] ?? _environment['Path'] ?? '';
    final names = _isWindows
        ? <String>['$command.exe', '$command.cmd', '$command.bat', command]
        : <String>[command];
    return pathValue
        .split(_isWindows ? ';' : ':')
        .where((value) => value.trim().isNotEmpty)
        .any(
          (directory) => names.any(
            (name) =>
                File(path.join(directory.replaceAll('"', ''), name))
                    .existsSync(),
          ),
        );
  }

  Future<void> install({
    required SkillItem skill,
    required Uint8List content,
    required List<String> targets,
  }) async {
    final validTargets = _validTargets(targets);
    if (validTargets.isEmpty) {
      throw const LocalInstallException('请至少选择一个 AI 工具');
    }
    _verifySha256(content, skill.sha256);

    for (final target in validTargets) {
      final destination = _targetPath(target, skillSlug(skill.name));
      await _installAtomically(
        destination: destination,
        fileName: skill.fileName,
        content: content,
      );
    }
  }

  Future<int> uninstall({
    required SkillItem skill,
    required List<String> targets,
  }) async {
    var removed = 0;
    for (final target in _validTargets(targets)) {
      final destination = Directory(_targetPath(target, skillSlug(skill.name)));
      if (!destination.existsSync()) continue;
      await destination.delete(recursive: true);
      removed += 1;
    }
    return removed;
  }

  bool isInstalled(SkillItem skill, String target) =>
      Directory(_targetPath(target, skillSlug(skill.name))).existsSync();

  String _targetPath(String target, String slug) {
    final relative = toolDirectories[target];
    if (relative == null) throw LocalInstallException('不支持的 AI 工具：$target');
    final destination = path.normalize(
      path.absolute(
        path.joinAll(<String>[_home, ...relative.split('/'), slug]),
      ),
    );
    if (!path.isWithin(_home, destination)) {
      throw const LocalInstallException('安装路径不安全');
    }
    return destination;
  }

  Future<void> _installAtomically({
    required String destination,
    required String fileName,
    required Uint8List content,
  }) async {
    final parent = Directory(path.dirname(destination));
    await parent.create(recursive: true);
    final temporary = await parent.createTemp('.skillport-install-');
    var moved = false;
    try {
      final lowerName = fileName.toLowerCase();
      if (lowerName.endsWith('.zip') || lowerName.endsWith('.skill')) {
        await _extractZip(content, temporary.path);
      } else {
        await File(path.join(temporary.path, 'SKILL.md'))
            .writeAsBytes(content, flush: true);
      }
      if (!File(path.join(temporary.path, 'SKILL.md')).existsSync()) {
        throw const LocalInstallException('Skill 包根目录缺少 SKILL.md');
      }
      final existing = Directory(destination);
      if (existing.existsSync()) await existing.delete(recursive: true);
      await temporary.rename(destination);
      moved = true;
    } on LocalInstallException {
      rethrow;
    } catch (error) {
      throw LocalInstallException('无法写入本机 Skill：$error');
    } finally {
      if (!moved && temporary.existsSync()) {
        await temporary.delete(recursive: true);
      }
    }
  }

  Future<void> _extractZip(Uint8List content, String destination) async {
    late Archive archive;
    try {
      archive = ZipDecoder().decodeBytes(content, verify: true);
    } catch (error) {
      throw LocalInstallException('Skill 压缩包无法读取：$error');
    }
    if (archive.length > 5000) {
      throw const LocalInstallException('Skill 压缩包文件数量过多');
    }
    final rootPrefix = commonArchiveRoot(
      archive.files.map((file) => file.name),
    );
    var totalSize = 0;
    for (final entry in archive.files) {
      final rawName = entry.name.replaceAll('\\', '/');
      _validateArchiveName(rawName);
      if (entry.isSymbolicLink) {
        throw const LocalInstallException('Skill 压缩包不能包含符号链接');
      }
      var relative = rawName;
      if (rootPrefix.isNotEmpty && relative.startsWith(rootPrefix)) {
        relative = relative.substring(rootPrefix.length);
      }
      relative = relative.replaceFirst(RegExp(r'^/+'), '');
      if (relative.isEmpty) continue;

      totalSize += entry.size;
      if (totalSize > 100 * 1024 * 1024) {
        throw const LocalInstallException('Skill 解压后不能超过 100MB');
      }
      final outputPath = path.normalize(
        path.absolute(
          path.joinAll(<String>[destination, ...relative.split('/')]),
        ),
      );
      if (!path.isWithin(destination, outputPath)) {
        throw const LocalInstallException('Skill 压缩包包含非法路径');
      }
      if (entry.isDirectory) {
        await Directory(outputPath).create(recursive: true);
        continue;
      }
      await Directory(path.dirname(outputPath)).create(recursive: true);
      await File(outputPath).writeAsBytes(entry.content, flush: true);
      if (!Platform.isWindows && entry.unixPermissions & 0x49 != 0) {
        final result = await Process.run('chmod', <String>['700', outputPath]);
        if (result.exitCode != 0) {
          throw LocalInstallException('无法设置脚本执行权限：$relative');
        }
      }
    }
  }

  void _verifySha256(Uint8List content, String expected) {
    if (expected.trim().isEmpty) {
      throw const LocalInstallException('云端没有提供 SHA-256，已停止安装');
    }
    final actual = sha256.convert(content).toString();
    if (actual.toLowerCase() != expected.trim().toLowerCase()) {
      throw const LocalInstallException('Skill 文件 SHA-256 校验失败');
    }
  }
}

String skillSlug(String value) {
  final buffer = StringBuffer();
  var pendingHyphen = false;
  for (final rune in value.trim().toLowerCase().runes) {
    final character = String.fromCharCode(rune);
    final isAsciiAlphaNumeric = RegExp(r'[a-z0-9]').hasMatch(character);
    final isUnicodeLetterOrNumber =
        !isAsciiAlphaNumeric &&
        RegExp(r'[\p{L}\p{N}]', unicode: true).hasMatch(character);
    if (isAsciiAlphaNumeric || isUnicodeLetterOrNumber) {
      if (pendingHyphen && buffer.isNotEmpty) buffer.write('-');
      buffer.write(character);
      pendingHyphen = false;
    } else if (buffer.isNotEmpty) {
      pendingHyphen = true;
    }
  }
  final result = buffer.toString();
  return result.isEmpty ? 'skillport-skill' : result;
}

String commonArchiveRoot(Iterable<String> names) {
  String? root;
  var found = false;
  for (final value in names) {
    final normalized = value
        .replaceAll('\\', '/')
        .replaceFirst(RegExp(r'^/+'), '');
    final parts = normalized.split('/');
    if (parts.length < 2 || parts.first.isEmpty) return '';
    found = true;
    root ??= parts.first;
    if (parts.first != root) return '';
  }
  return found && root != null ? '$root/' : '';
}

void _validateArchiveName(String value) {
  if (value.startsWith('/') || RegExp(r'^[A-Za-z]:').hasMatch(value)) {
    throw const LocalInstallException('Skill 压缩包包含绝对路径');
  }
  if (value.split('/').contains('..')) {
    throw const LocalInstallException('Skill 压缩包包含非法路径');
  }
}

List<String> _validTargets(List<String> targets) =>
    targets.where(toolDirectories.containsKey).toSet().toList()..sort();

class LocalInstallException implements Exception {
  const LocalInstallException(this.message);

  final String message;

  @override
  String toString() => message;
}
