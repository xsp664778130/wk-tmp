import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;

import 'models.dart';

class AuthenticationResult {
  const AuthenticationResult({required this.token, required this.user});

  final String token;
  final SkillPortUser user;
}

class PullResult {
  const PullResult({required this.skill, required this.created});

  final SkillItem skill;
  final bool created;
}

class SkillPortApi {
  SkillPortApi({
    String baseUrl = 'https://www.jmuyuer.com',
    http.Client? httpClient,
  }) : baseUri = Uri.parse(baseUrl.replaceFirst(RegExp(r'/$'), '')),
       _http = httpClient ?? http.Client();

  final Uri baseUri;
  final http.Client _http;
  String? token;

  Map<String, String> get _jsonHeaders => <String, String>{
    'accept': 'application/json',
    'content-type': 'application/json; charset=utf-8',
    if (token != null) 'cookie': 'skillport_session=$token',
    'user-agent': 'SkillPort-Flutter/1.0.21',
  };

  Map<String, String> get sessionHeaders => <String, String>{
    if (token != null) 'cookie': 'skillport_session=$token',
  };

  Uri uri(String path) => baseUri.resolve(path);

  Future<AuthenticationResult> login(String email, String password) async {
    final response = await _http.post(
      uri('/api/auth/login'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, String>{
        'email': email.trim(),
        'password': password,
      }),
    );
    final data = _decodeObject(response);
    return AuthenticationResult(
      token: _sessionToken(response),
      user: SkillPortUser.fromJson(_object(data['user'])),
    );
  }

  Future<AuthenticationResult> register({
    required String email,
    required String displayName,
    required String password,
  }) async {
    final response = await _http.post(
      uri('/api/auth/register'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, String>{
        'email': email.trim(),
        'displayName': displayName.trim(),
        'password': password,
      }),
    );
    final data = _decodeObject(response);
    return AuthenticationResult(
      token: _sessionToken(response),
      user: SkillPortUser.fromJson(_object(data['user'])),
    );
  }

  Future<SkillPortUser> me() async {
    final response = await _http.get(
      uri('/api/auth/me'),
      headers: _jsonHeaders,
    );
    return SkillPortUser.fromJson(_object(_decodeObject(response)['user']));
  }

  Future<void> logout() async {
    final response = await _http.post(
      uri('/api/auth/logout'),
      headers: _jsonHeaders,
    );
    _ensureSuccess(response);
  }

  Future<List<SkillItem>> privateSkills() async {
    final response = await _http.get(uri('/api/skills'), headers: _jsonHeaders);
    return _list(_decodeObject(response)['skills'])
        .map((item) => SkillItem.fromPrivateJson(_object(item)))
        .toList();
  }

  Future<List<SkillItem>> publicSkills() async {
    final response = await _http.get(
      uri('/api/public-skills'),
      headers: _jsonHeaders,
    );
    return _list(_decodeObject(response)['skills'])
        .map((item) => SkillItem.fromPublicJson(_object(item)))
        .toList();
  }

  Future<SkillItem> uploadSkill({
    required String filePath,
    required String name,
    required String description,
    required String detail,
    required List<String> usageSteps,
    required String category,
    String? avatarPath,
  }) async {
    final request = http.MultipartRequest('POST', uri('/api/skills'))
      ..headers.addAll(sessionHeaders)
      ..fields['name'] = name.trim()
      ..fields['description'] = description.trim()
      ..fields['detail'] = detail.trim()
      ..fields['usageSteps'] = usageSteps.join('\n')
      ..fields['category'] = category
      ..files.add(await http.MultipartFile.fromPath('file', filePath));
    if (avatarPath != null && avatarPath.isNotEmpty) {
      request.files.add(
        await http.MultipartFile.fromPath('avatar', avatarPath),
      );
    }
    final streamed = await _http.send(request);
    final response = await http.Response.fromStream(streamed);
    return SkillItem.fromPrivateJson(_decodeObject(response));
  }

  Future<SkillItem> updateNote(String skillId, String note) async {
    final response = await _http.patch(
      uri('/api/skills'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, String>{'id': skillId, 'note': note}),
    );
    return SkillItem.fromPrivateJson(_decodeObject(response));
  }

  Future<SkillItem> updateCategory(String skillId, String category) async {
    final response = await _http.patch(
      uri('/api/skills/${Uri.encodeComponent(skillId)}'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, String>{'category': category}),
    );
    return SkillItem.fromPrivateJson(_decodeObject(response));
  }

  Future<SkillItem> updateDetails(
    String skillId, {
    required String name,
    required String description,
    required String detail,
    required List<String> usageSteps,
  }) async {
    final response = await _http.patch(
      uri('/api/skills/${Uri.encodeComponent(skillId)}'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, dynamic>{
        'name': name.trim(),
        'description': description.trim(),
        'detail': detail.trim(),
        'usageSteps': usageSteps,
      }),
    );
    return SkillItem.fromPrivateJson(_decodeObject(response));
  }

  Future<void> shareSkill(String skillId) async {
    final response = await _http.post(
      uri('/api/public-skills'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, String>{'skillId': skillId}),
    );
    _ensureSuccess(response);
  }

  Future<void> unpublishSkill(SkillItem skill) async {
    final path = skill.isPublic
        ? '/api/public-skills/${Uri.encodeComponent(skill.id)}'
        : '/api/public-skills/source/${Uri.encodeComponent(skill.id)}';
    final response = await _http.delete(uri(path), headers: _jsonHeaders);
    _ensureSuccess(response);
  }

  Future<void> deleteSkill(String skillId) async {
    final response = await _http.delete(
      uri('/api/skills/${Uri.encodeComponent(skillId)}'),
      headers: _jsonHeaders,
    );
    _ensureSuccess(response);
  }

  Future<void> submitFeedback({
    required String kind,
    required String content,
  }) async {
    final response = await _http.post(
      uri('/api/feedback'),
      headers: _jsonHeaders,
      body: jsonEncode(<String, String>{
        'kind': kind.trim(),
        'content': content.trim(),
      }),
    );
    _ensureSuccess(response);
  }

  Future<FeedbackPage> feedbackPage({int page = 1, int size = 6}) async {
    final response = await _http.get(
      uri('/api/feedback?page=$page&size=$size'),
      headers: _jsonHeaders,
    );
    return FeedbackPage.fromJson(_decodeObject(response));
  }

  Future<PullResult> pullSkill(String publicSkillId) async {
    final response = await _http.post(
      uri('/api/public-skills/${Uri.encodeComponent(publicSkillId)}/pull'),
      headers: _jsonHeaders,
    );
    final data = _decodeObject(response);
    return PullResult(
      skill: SkillItem.fromPrivateJson(_object(data['skill'])),
      created: data['created'] == true,
    );
  }

  Future<Uint8List> downloadSkill(SkillItem skill) async {
    final response = await _http.get(
      uri('/api/skills/${Uri.encodeComponent(skill.id)}/file'),
      headers: <String, String>{
        ...sessionHeaders,
        'accept': 'application/octet-stream',
        'user-agent': 'SkillPort-Flutter/1.0.21',
      },
    );
    _ensureSuccess(response);
    if (skill.sizeBytes > 0 && response.bodyBytes.length != skill.sizeBytes) {
      throw const ApiException('Skill 文件大小校验失败，请重新下载');
    }
    return response.bodyBytes;
  }

  String? absoluteAvatarUrl(SkillItem skill) {
    final value = skill.avatarUrl;
    if (value == null || value.isEmpty) return null;
    return uri(value).toString();
  }

  void close() => _http.close();

  Map<String, dynamic> _decodeObject(http.Response response) {
    _ensureSuccess(response);
    if (response.bodyBytes.isEmpty) return <String, dynamic>{};
    final value = jsonDecode(utf8.decode(response.bodyBytes));
    if (value is! Map<String, dynamic>) {
      throw const ApiException('云端返回了无法识别的数据');
    }
    return value;
  }

  void _ensureSuccess(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) return;
    String message = '请求失败（${response.statusCode}）';
    try {
      final value = jsonDecode(utf8.decode(response.bodyBytes));
      if (value is Map) {
        message =
            (value['detail'] ?? value['error'] ?? value['message'] ?? message)
                .toString();
      }
    } catch (_) {
      if (response.reasonPhrase?.isNotEmpty == true) {
        message = response.reasonPhrase!;
      }
    }
    throw ApiException(message, statusCode: response.statusCode);
  }

  String _sessionToken(http.Response response) {
    final setCookie = response.headers['set-cookie'] ?? '';
    const marker = 'skillport_session=';
    final start = setCookie.indexOf(marker);
    if (start < 0) throw const ApiException('云端没有返回登录会话');
    final valueStart = start + marker.length;
    final semicolon = setCookie.indexOf(';', valueStart);
    final value = setCookie
        .substring(valueStart, semicolon < 0 ? setCookie.length : semicolon)
        .trim();
    if (value.isEmpty) throw const ApiException('云端没有返回登录会话');
    return value;
  }
}

class ApiException implements Exception {
  const ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

Map<String, dynamic> _object(dynamic value) =>
    value is Map<String, dynamic> ? value : <String, dynamic>{};

List<dynamic> _list(dynamic value) => value is List ? value : const <dynamic>[];
