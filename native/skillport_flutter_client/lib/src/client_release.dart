import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

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
  }) : _baseUri = Uri.parse(baseUrl.replaceFirst(RegExp(r'/$'), '')),
       _http = httpClient ?? http.Client(),
       _ownsClient = httpClient == null;

  final Uri _baseUri;
  final http.Client _http;
  final bool _ownsClient;

  Future<ClientReleaseInfo> fetchLatest() async {
    final response = await _http
        .get(
          _baseUri.resolve('/bridge/client/latest.json'),
          headers: const <String, String>{
            'accept': 'application/json',
            'user-agent': 'SkillPort-Flutter/1.0.14',
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

  Future<void> openInstaller(ClientReleaseInfo release) async {
    if (Platform.isMacOS) {
      await Process.start('/usr/bin/open', <String>[
        release.macosUrl.toString(),
      ]);
      return;
    }
    if (Platform.isWindows) {
      await Process.start('explorer.exe', <String>[
        release.windowsUrl.toString(),
      ]);
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
