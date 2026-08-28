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
  'cursor': '.cursor/skills',
};

const toolLabels = <String, String>{
  'codex': 'Codex',
  'qoder': 'Qoder',
  'opencode': 'OpenCode',
  'claude': 'Claude Code',
  'cursor': 'Cursor',
};

const toolMarks = <String, String>{
  'codex': 'CX',
  'qoder': 'Q',
  'opencode': 'OC',
  'claude': 'CC',
  'cursor': 'CU',
};

const skillPortOriginFile = '.skillport-origin';

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
      case 'cursor':
        return _macAppExists('Cursor') || _windowsCursorExists();
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

  bool _windowsCursorExists() {
    if (!_isWindows) return false;
    final roots = <String?>[
      _environment['LOCALAPPDATA'],
      _environment['PROGRAMFILES'],
      _environment['PROGRAMFILES(X86)'],
    ].whereType<String>();
    return roots.any(
      (root) => <String>[
        path.join(root, 'Cursor', 'Cursor.exe'),
        path.join(root, 'cursor', 'Cursor.exe'),
        path.join(root, 'Programs', 'Cursor', 'Cursor.exe'),
        path.join(root, 'Programs', 'cursor', 'Cursor.exe'),
      ].any((candidate) => File(candidate).existsSync()),
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
        originSkillId: skill.id,
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

  Future<List<LocalSkillItem>> scanLocalSkills({
    Iterable<String>? toolIds,
  }) async {
    final selected = (toolIds ?? toolDirectories.keys)
        .where(toolDirectories.containsKey)
        .toSet();
    final result = <LocalSkillItem>[];
    for (final toolId in selected) {
      final root = Directory(_toolRoot(toolId));
      if (!root.existsSync()) continue;
      for (final entity in root.listSync(followLinks: false)) {
        if (entity is! Directory ||
            path.basename(entity.path).startsWith('.skillport-install-')) {
          continue;
        }
        final skillFile = _findSkillFile(entity, depth: 0);
        if (skillFile == null) continue;
        final metadata = await _readSkillMetadata(skillFile);
        final marker = File(path.join(entity.path, skillPortOriginFile));
        final origin = marker.existsSync()
            ? (await marker.readAsString()).trim()
            : '';
        result.add(
          LocalSkillItem(
            toolId: toolId,
            slug: path.basename(entity.path),
            name: metadata.$1,
            description: metadata.$2,
            directory: path.normalize(entity.path),
            originSkillId: origin.isEmpty ? null : origin,
          ),
        );
      }
    }
    result.sort((left, right) {
      final byTool = left.toolId.compareTo(right.toolId);
      return byTool != 0
          ? byTool
          : left.name.toLowerCase().compareTo(right.name.toLowerCase());
    });
    return result;
  }

  Future<bool> uninstallLocalSkill(LocalSkillItem skill) async {
    final directory = _validatedLocalSkillDirectory(skill);
    final target = Directory(directory);
    if (!target.existsSync()) return false;
    await target.delete(recursive: true);
    return true;
  }

  Future<void> openLocalSkillFolder(LocalSkillItem skill) async {
    final directory = _validatedLocalSkillDirectory(skill);
    try {
      if (_isMacOS) {
        await Process.start('open', <String>[
          directory,
        ], mode: ProcessStartMode.detached);
      } else if (_isWindows) {
        await Process.start('explorer.exe', <String>[
          directory,
        ], mode: ProcessStartMode.detached);
      } else {
        throw const LocalInstallException('当前系统暂不支持打开本地文件夹');
      }
    } on LocalInstallException {
      rethrow;
    } catch (error) {
      throw LocalInstallException('无法打开本地文件夹：$error');
    }
  }

  Future<String> readLocalSkillManifest(LocalSkillItem skill) async {
    final directory = _validatedLocalSkillDirectory(skill);
    final manifest = _findSkillFile(Directory(directory), depth: 0);
    if (manifest == null) {
      throw const LocalInstallException('本机 Skill 中没有找到 SKILL.md');
    }
    try {
      if (await manifest.length() > 512 * 1024) {
        throw const LocalInstallException('SKILL.md 超过 512KB，无法预览');
      }
      return await manifest.readAsString();
    } on LocalInstallException {
      rethrow;
    } catch (error) {
      throw LocalInstallException('无法读取 SKILL.md：$error');
    }
  }

  String _validatedLocalSkillDirectory(LocalSkillItem skill) {
    final root = path.normalize(path.absolute(_toolRoot(skill.toolId)));
    final directory = path.normalize(path.absolute(skill.directory));
    final type = FileSystemEntity.typeSync(directory, followLinks: false);
    if (!path.isWithin(root, directory) ||
        path.dirname(directory) != root ||
        type != FileSystemEntityType.directory) {
      throw const LocalInstallException('本机 Skill 路径不安全，请重新识别');
    }
    return directory;
  }

  String _toolRoot(String target) {
    final relative = toolDirectories[target];
    if (relative == null) throw LocalInstallException('不支持的 AI 工具：$target');
    return path.normalize(
      path.absolute(path.joinAll(<String>[_home, ...relative.split('/')])),
    );
  }

  File? _findSkillFile(Directory directory, {required int depth}) {
    if (depth > 3) return null;
    for (final entity in directory.listSync(followLinks: false)) {
      if (entity is File &&
          path.basename(entity.path).toLowerCase() == 'skill.md') {
        return entity;
      }
    }
    if (depth == 3) return null;
    for (final entity in directory.listSync(followLinks: false)) {
      if (entity is Directory) {
        final nested = _findSkillFile(entity, depth: depth + 1);
        if (nested != null) return nested;
      }
    }
    return null;
  }

  Future<(String, String)> _readSkillMetadata(File file) async {
    var content = await file.readAsString();
    if (content.length > 128 * 1024) content = content.substring(0, 128 * 1024);
    var name = '';
    var description = '';
    final lines = content.split(RegExp(r'\r?\n'));
    if (lines.isNotEmpty && lines.first.trim() == '---') {
      for (var index = 1; index < lines.length; index += 1) {
        final line = lines[index].trim();
        if (line == '---') break;
        final separator = line.indexOf(':');
        if (separator < 1) continue;
        final key = line.substring(0, separator).trim().toLowerCase();
        final value = _unquote(line.substring(separator + 1).trim());
        if (key == 'name') name = value;
        if (key == 'description') description = value;
      }
    }
    if (name.isEmpty) {
      final heading = lines.cast<String?>().firstWhere(
        (line) => line?.trimLeft().startsWith('# ') == true,
        orElse: () => null,
      );
      name =
          heading?.trim().substring(2).trim() ??
          path.basename(file.parent.path);
    }
    return (name, description.isEmpty ? '本机 Skill' : description);
  }

  String _unquote(String value) {
    if (value.length >= 2 &&
        ((value.startsWith('"') && value.endsWith('"')) ||
            (value.startsWith("'") && value.endsWith("'")))) {
      return value.substring(1, value.length - 1);
    }
    return value;
  }

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
    required String originSkillId,
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
      await File(path.join(temporary.path, skillPortOriginFile))
          .writeAsString(originSkillId, flush: true);
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
    final manifests = archive.files.where((entry) {
      if (entry.isDirectory) return false;
      final normalized = entry.name
          .replaceAll('\\', '/')
          .replaceFirst(RegExp(r'^/+'), '');
      return !_isIgnoredArchiveMetadata(normalized) &&
          normalized.split('/').last.toLowerCase() == 'skill.md';
    }).toList();
    if (manifests.length != 1) {
      throw const LocalInstallException('Skill 压缩包必须且只能包含一个 SKILL.md');
    }
    final manifestName = manifests.single.name
        .replaceAll('\\', '/')
        .replaceFirst(RegExp(r'^/+'), '');
    final manifestSeparator = manifestName.lastIndexOf('/');
    final rootPrefix = manifestSeparator < 0
        ? ''
        : manifestName.substring(0, manifestSeparator + 1);
    var totalSize = 0;
    for (final entry in archive.files) {
      final rawName = entry.name.replaceAll('\\', '/');
      _validateArchiveName(rawName);
      if (entry.isSymbolicLink) {
        throw const LocalInstallException('Skill 压缩包不能包含符号链接');
      }
      totalSize += entry.size;
      if (totalSize > 100 * 1024 * 1024) {
        throw const LocalInstallException('Skill 解压后不能超过 100MB');
      }
      var relative = rawName.replaceFirst(RegExp(r'^/+'), '');
      if (_isIgnoredArchiveMetadata(relative)) continue;
      if (rootPrefix.isNotEmpty) {
        if (!relative.startsWith(rootPrefix)) continue;
        relative = relative.substring(rootPrefix.length);
      }
      if (relative.isEmpty) continue;
      if (relative.toLowerCase() == 'skill.md') relative = 'SKILL.md';

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

bool _isIgnoredArchiveMetadata(String value) {
  final parts = value
      .replaceAll('\\', '/')
      .split('/')
      .where((part) => part.isNotEmpty);
  return parts.any(
    (part) =>
        part.toLowerCase() == '__macosx' ||
        part == '.DS_Store' ||
        part.startsWith('._'),
  );
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
