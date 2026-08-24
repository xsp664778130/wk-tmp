import 'dart:io';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

abstract interface class SessionStore {
  Future<String?> readToken();
  Future<void> writeToken(String token);
  Future<void> clearToken();
}

SessionStore createSessionStore() {
  if (Platform.isMacOS) return MacFileSessionStore();
  return const SecureSessionStore();
}

class SessionStoreException implements Exception {
  const SessionStoreException(this.message);

  final String message;

  @override
  String toString() => message;
}

class SecureSessionStore implements SessionStore {
  const SecureSessionStore();

  static const _tokenKey = 'skillport_session_token';
  static const _storage = FlutterSecureStorage();

  @override
  Future<String?> readToken() => _storage.read(key: _tokenKey);

  @override
  Future<void> writeToken(String token) =>
      _storage.write(key: _tokenKey, value: token);

  @override
  Future<void> clearToken() => _storage.delete(key: _tokenKey);
}

/// Stores the macOS session without Keychain access.
///
/// The public macOS build is ad-hoc signed until a Developer ID certificate is
/// configured. Such a build cannot reliably access a Keychain access group and
/// fails with Security error -34018. Keep the random server session token in a
/// private per-user directory instead; a future Developer ID build can migrate
/// this back to [SecureSessionStore].
class MacFileSessionStore implements SessionStore {
  MacFileSessionStore({Directory? supportDirectory})
    : _supportDirectory = supportDirectory;

  static const _fileName = 'session.token';
  final Directory? _supportDirectory;

  Directory get _directory {
    if (_supportDirectory case final directory?) return directory;
    final home = Platform.environment['HOME'];
    if (home == null || home.isEmpty) {
      throw const SessionStoreException('无法确定当前 macOS 用户目录，请重新登录系统后再试。');
    }
    return Directory('$home/Library/Application Support/SkillPort');
  }

  File get _tokenFile => File('${_directory.path}/$_fileName');

  @override
  Future<String?> readToken() async {
    try {
      final type = await FileSystemEntity.type(
        _tokenFile.path,
        followLinks: false,
      );
      if (type == FileSystemEntityType.notFound) return null;
      if (type != FileSystemEntityType.file) {
        throw const SessionStoreException('本机会话文件异常，请删除后重新登录。');
      }
      await _chmod('600', _tokenFile.path);
      final token = (await _tokenFile.readAsString()).trim();
      return token.isEmpty ? null : token;
    } on SessionStoreException {
      rethrow;
    } catch (_) {
      throw const SessionStoreException('无法读取本机登录状态，请检查用户目录权限。');
    }
  }

  @override
  Future<void> writeToken(String token) async {
    if (token.trim().isEmpty) {
      throw const SessionStoreException('云端没有返回有效的登录令牌。');
    }
    File? temporary;
    try {
      await _directory.create(recursive: true);
      await _chmod('700', _directory.path);
      temporary = File(
        '${_directory.path}/.$_fileName.$pid.${DateTime.now().microsecondsSinceEpoch}',
      );
      await temporary.writeAsString(token, flush: true);
      await _chmod('600', temporary.path);
      await temporary.rename(_tokenFile.path);
      await _chmod('600', _tokenFile.path);
    } on SessionStoreException {
      rethrow;
    } catch (_) {
      if (temporary != null && await temporary.exists()) {
        await temporary.delete();
      }
      throw const SessionStoreException('无法保存登录状态，请检查当前用户目录权限后重试。');
    }
  }

  @override
  Future<void> clearToken() async {
    try {
      final type = await FileSystemEntity.type(
        _tokenFile.path,
        followLinks: false,
      );
      if (type != FileSystemEntityType.notFound) await _tokenFile.delete();
    } catch (_) {
      throw const SessionStoreException('无法清除本机登录状态，请检查当前用户目录权限。');
    }
  }

  Future<void> _chmod(String mode, String path) async {
    final result = await Process.run('/bin/chmod', <String>[mode, path]);
    if (result.exitCode != 0) {
      throw const SessionStoreException('无法保护本机登录状态，请检查当前用户目录权限。');
    }
  }
}
