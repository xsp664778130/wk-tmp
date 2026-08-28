import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

typedef InstallerLauncher = Future<void> Function(
  String operatingSystem,
  String installerPath,
);

class ClientReleaseInfo {
  const ClientReleaseInfo({
    required this.version,
    required this.date,
    required this.title,
    required this.changes,
    required this.macosUrl,
    required this.windowsUrl,
  });

  final String version;
  final String date;
  final String title;
  final List<String> changes;
  final Uri macosUrl;
  final Uri windowsUrl;

  factory ClientReleaseInfo.fromJson(Map<String, dynamic> json) {
    return ClientReleaseInfo(
      version: _requiredString(json, 'version'),
      date: _requiredString(json, 'date'),
      title: _requiredString(json, 'title'),
      changes: (json['changes'] as List<dynamic>? ?? const <dynamic>[])
          .map((item) => item.toString().trim())
          .where((item) => item.isNotEmpty)
          .toList(growable: false),
      macosUrl: _secureUri(json, 'macosUrl'),
      windowsUrl: _secureUri(json, 'windowsUrl'),
    );
  }

  static String _requiredString(Map<String, dynamic> json, String key) {
    final value = json[key]?.toString().trim() ?? '';
    if (value.isEmpty) throw FormatException('缺少更新字段：$key');
    return value;
  }

  static Uri _secureUri(Map<String, dynamic> json, String key) {
    final uri = Uri.tryParse(_requiredString(json, key));
    if (uri == null || uri.scheme != 'https' || uri.host.isEmpty) {
      throw FormatException('更新地址不安全：$key');
    }
    return uri;
  }
}

class ClientReleaseService {
  ClientReleaseService({
    String baseUrl = 'https://www.jmuyuer.com',
    http.Client? httpClient,
    String? operatingSystem,
    this.installerLauncher,
    Future<Directory> Function()? temporaryDirectoryFactory,
  }) : _baseUri = Uri.parse(baseUrl.replaceFirst(RegExp(r'/$'), '')),
       _http = httpClient ?? http.Client(),
       _ownsClient = httpClient == null,
       _operatingSystem = operatingSystem ?? Platform.operatingSystem,
       _temporaryDirectoryFactory =
           temporaryDirectoryFactory ??
           (() => Directory.systemTemp.createTemp('skillport-update-'));

  final Uri _baseUri;
  final http.Client _http;
  final bool _ownsClient;
  final String _operatingSystem;
  final InstallerLauncher? installerLauncher;
  final Future<Directory> Function() _temporaryDirectoryFactory;

  Future<ClientReleaseInfo> fetchLatest() async {
    final response = await _http
        .get(
          _baseUri.resolve('/bridge/client/latest.json'),
          headers: const <String, String>{
            'accept': 'application/json',
            'user-agent': 'SkillPort-Flutter/1.0.31',
          },
        )
        .timeout(const Duration(seconds: 8));
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw HttpException('检查更新失败（${response.statusCode}）');
    }
    final decoded = jsonDecode(utf8.decode(response.bodyBytes));
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('更新信息格式不正确');
    }
    final release = ClientReleaseInfo.fromJson(decoded);
    if (release.macosUrl.host != _baseUri.host ||
        release.windowsUrl.host != _baseUri.host) {
      throw const FormatException('更新下载地址与服务地址不一致');
    }
    return release;
  }

  Future<File> downloadAndLaunch(
    ClientReleaseInfo release, {
    void Function(double progress)? onProgress,
  }) async {
    final (downloadUri, fileName) = switch (_operatingSystem) {
      'macos' => (release.macosUrl, 'SkillPort-Bridge.pkg'),
      'windows' => (release.windowsUrl, 'SkillPort-Setup.exe'),
      _ => throw UnsupportedError('当前系统暂不支持自动更新'),
    };
    if (downloadUri.host != _baseUri.host || downloadUri.scheme != 'https') {
      throw const FormatException('更新下载地址与服务地址不一致');
    }

    final request = http.Request('GET', downloadUri)
      ..followRedirects = false
      ..headers.addAll(const <String, String>{
        'accept': 'application/octet-stream',
        'user-agent': 'SkillPort-Flutter-Updater/1.0.31',
      });
    final response = await _http
        .send(request)
        .timeout(const Duration(seconds: 12));
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw HttpException('下载安装包失败（${response.statusCode}）');
    }

    final directory = await _temporaryDirectoryFactory();
    final installer = File('${directory.path}${Platform.pathSeparator}$fileName');
    final sink = installer.openWrite();
    var received = 0;
    final total = response.contentLength;
    onProgress?.call(0);
    try {
      await for (final chunk in response.stream.timeout(
        const Duration(seconds: 30),
      )) {
        sink.add(chunk);
        received += chunk.length;
        if (total != null && total > 0) {
          onProgress?.call((received / total).clamp(0, 1).toDouble());
        }
      }
      await sink.flush();
      await sink.close();
      if (received == 0 || (total != null && received != total)) {
        throw const HttpException('安装包下载不完整');
      }
      onProgress?.call(1);
      if (installerLauncher != null) {
        await installerLauncher!(_operatingSystem, installer.path);
      } else {
        await _launchInstaller(installer.path);
      }
      return installer;
    } catch (_) {
      await sink.close();
      if (await installer.exists()) await installer.delete();
      rethrow;
    }
  }

  Future<void> _launchInstaller(String installerPath) async {
    if (_operatingSystem == 'macos') {
      await Process.start(
        '/usr/bin/open',
        <String>[installerPath],
        mode: ProcessStartMode.detached,
      );
      return;
    }
    if (_operatingSystem == 'windows') {
      await Process.start(
        installerPath,
        const <String>[
          '/SILENT',
          '/SUPPRESSMSGBOXES',
          '/CLOSEAPPLICATIONS',
          '/RESTARTAPPLICATIONS',
        ],
        mode: ProcessStartMode.detached,
      );
      return;
    }
    throw UnsupportedError('当前系统暂不支持自动更新');
  }

  void close() {
    if (_ownsClient) _http.close();
  }
}

bool isNewerVersion(String latest, String current) {
  final latestParts = _versionParts(latest);
  final currentParts = _versionParts(current);
  if (latestParts == null || currentParts == null) return false;
  final length = latestParts.length > currentParts.length
      ? latestParts.length
      : currentParts.length;
  for (var index = 0; index < length; index++) {
    final latestPart = index < latestParts.length ? latestParts[index] : 0;
    final currentPart = index < currentParts.length ? currentParts[index] : 0;
    if (latestPart != currentPart) return latestPart > currentPart;
  }
  return false;
}

List<int>? _versionParts(String value) {
  final normalized = value.trim().replaceFirst(RegExp(r'^[vV]'), '');
  final core = normalized.split(RegExp(r'[-+]')).first;
  if (core.isEmpty) return null;
  final parts = <int>[];
  for (final segment in core.split('.')) {
    final number = int.tryParse(segment);
    if (number == null || number < 0) return null;
    parts.add(number);
  }
  return parts;
}
