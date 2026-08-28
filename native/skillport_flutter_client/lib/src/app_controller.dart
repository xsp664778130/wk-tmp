import 'dart:async';

import 'package:flutter/foundation.dart';

import 'api_client.dart';
import 'app_theme.dart';
import 'local_installer.dart';
import 'models.dart';
import 'session_store.dart';

class FeedbackEvent {
  const FeedbackEvent(this.id, this.message, {this.error = false});

  final int id;
  final String message;
  final bool error;
}

class AppController extends ChangeNotifier {
  AppController({
    required this._api,
    required this._sessionStore,
    required this._installer,
  });

  final SkillPortApi _api;
  final SessionStore _sessionStore;
  final LocalInstaller _installer;

  SkillPortUser? user;
  LibraryMode mode = LibraryMode.publicPool;
  String activeCategory = '全部技能';
  String query = '';
  List<SkillItem> privateSkills = const <SkillItem>[];
  List<SkillItem> publicSkills = const <SkillItem>[];
  List<ToolTarget> tools = const <ToolTarget>[];
  List<LocalSkillItem> localSkills = const <LocalSkillItem>[];
  String? selectedLocalToolId;
  final List<LocalActivity> activities = <LocalActivity>[];
  bool initializing = true;
  bool busy = false;
  String busyLabel = '';
  FeedbackEvent? feedback;
  SkillPortThemePreset themePreset = SkillPortThemePreset.midnight;
  int _feedbackId = 0;

  bool get signedIn => user != null;
  Map<String, String> get imageHeaders => _api.sessionHeaders;

  List<SkillItem> get visibleSkills {
    if (mode == LibraryMode.localWorkspace) return const <SkillItem>[];
    final source = mode == LibraryMode.publicPool
        ? publicSkills
        : privateSkills;
    final normalized = query.trim().toLowerCase();
    return source.where((skill) {
      final categoryMatches =
          activeCategory == '全部技能' || skill.category == activeCategory;
      final searchMatches =
          normalized.isEmpty ||
          '${skill.name} ${skill.description} ${skill.detail} ${skill.usageSteps.join(' ')} ${skill.category} ${skill.note}'
              .toLowerCase()
              .contains(normalized);
      return categoryMatches && searchMatches;
    }).toList();
  }

  List<ToolTarget> get detectedTools =>
      tools.where((tool) => tool.detected).toList(growable: false);

  List<LocalSkillItem> get visibleLocalSkills {
    final normalized = query.trim().toLowerCase();
    return localSkills.where((skill) {
      final toolMatches = skill.toolId == selectedLocalToolId;
      final searchMatches = normalized.isEmpty ||
          '${skill.name} ${skill.description} ${skill.slug} ${skill.directory}'
              .toLowerCase()
              .contains(normalized);
      return toolMatches && searchMatches;
    }).toList(growable: false);
  }

  Future<void> initialize() async {
    try {
      themePreset = await ThemePreferenceStore.read();
      notifyListeners();
      await _refreshLocalWorkspace();
      final token = await _sessionStore.readToken();
      if (token != null && token.isNotEmpty) {
        _api.token = token;
        try {
          user = await _api.me();
          await refresh(showBusy: false);
        } on ApiException catch (error) {
          if (error.statusCode == 401 || error.statusCode == 403) {
            await _sessionStore.clearToken();
            _api.token = null;
          } else {
            rethrow;
          }
        }
      }
    } catch (error) {
      _show('客户端初始化失败：$error', error: true);
    } finally {
      initializing = false;
      notifyListeners();
    }
  }

  Future<void> setThemePreset(SkillPortThemePreset value) async {
    if (themePreset == value) return;
    themePreset = value;
    notifyListeners();
    await ThemePreferenceStore.write(value);
  }

  Future<bool> login(String email, String password) async =>
      _authenticate(() => _api.login(email, password));

  Future<bool> register({
    required String email,
    required String displayName,
    required String password,
  }) async => _authenticate(
    () => _api.register(
      email: email,
      displayName: displayName,
      password: password,
    ),
  );

  Future<bool> _authenticate(
    Future<AuthenticationResult> Function() request,
  ) async {
    return _perform('正在安全登录…', () async {
      final result = await request();
      if (result.token.isEmpty) throw const ApiException('云端没有返回登录令牌');
      await _sessionStore.writeToken(result.token);
      _api.token = result.token;
      user = result.user;
      await refresh(showBusy: false);
      _show('欢迎进入你的 SkillPort 客户端');
    });
  }

  Future<void> logout() async {
    await _perform('正在退出…', () async {
      try {
        await _api.logout();
      } finally {
        await _sessionStore.clearToken();
        _api.token = null;
        user = null;
        privateSkills = const <SkillItem>[];
        publicSkills = const <SkillItem>[];
        mode = LibraryMode.publicPool;
        activeCategory = '全部技能';
      }
    });
  }

  Future<void> refresh({bool showBusy = true}) async {
    if (!signedIn) return;
    Future<void> load() async {
      final results = await Future.wait(<Future<List<SkillItem>>>[
        _api.privateSkills(),
        _api.publicSkills(),
      ]);
      privateSkills = results[0];
      publicSkills = results[1];
      await _refreshLocalWorkspace();
    }

    if (showBusy) {
      await _perform('正在同步云端…', load, successMessage: '数据已同步');
    } else {
      await load();
      notifyListeners();
    }
  }

  void setMode(LibraryMode value) {
    mode = value;
    activeCategory = '全部技能';
    query = '';
    notifyListeners();
  }

  void selectLocalTool(String toolId) {
    if (!detectedTools.any((tool) => tool.id == toolId)) return;
    selectedLocalToolId = toolId;
    mode = LibraryMode.localWorkspace;
    activeCategory = '全部技能';
    query = '';
    notifyListeners();
  }

  void setCategory(String value) {
    activeCategory = value;
    notifyListeners();
  }

  void setQuery(String value) {
    query = value;
    notifyListeners();
  }

  Future<bool> upload({
    required String filePath,
    required String name,
    required String description,
    required String detail,
    required List<String> usageSteps,
    required String category,
    required String note,
    String? avatarPath,
  }) async {
    return _perform('正在检查并上传 Skill…', () async {
      var created = await _api.uploadSkill(
        filePath: filePath,
        name: name,
        description: description,
        detail: detail,
        usageSteps: usageSteps,
        category: category,
        avatarPath: avatarPath,
      );
      if (note.trim().isNotEmpty) {
        created = await _api.updateNote(created.id, note.trim());
      }
      privateSkills = <SkillItem>[
        created,
        ...privateSkills.where((item) => item.id != created.id),
      ];
      mode = LibraryMode.privateSpace;
      activeCategory = '全部技能';
    }, successMessage: 'Skill 已通过结构检查并保存');
  }

  Future<bool> updateNote(SkillItem skill, String note) async {
    return _perform('正在保存备注…', () async {
      final updated = await _api.updateNote(skill.id, note.trim());
      privateSkills = privateSkills
          .map((item) => item.id == updated.id ? updated : item)
          .toList();
    }, successMessage: '备注已保存，仅当前账户可见');
  }

  Future<bool> updateCategory(SkillItem skill, String category) async {
    return _perform('正在保存分类…', () async {
      final updated = await _api.updateCategory(skill.id, category);
      privateSkills = privateSkills
          .map((item) => item.id == updated.id ? updated : item)
          .toList();
      publicSkills = publicSkills
          .map((item) => item.sourceSkillId == skill.id
              ? item.copyWith(category: updated.category)
              : item)
          .toList();
    }, successMessage: skill.shared ? '分类已保存，并同步到 Skill 公有池' : 'Skill 分类已保存');
  }

  Future<bool> updateDetails(
    SkillItem skill, {
    required String name,
    required String description,
    required String detail,
    required List<String> usageSteps,
  }) async {
    return _perform('正在保存 Skill 详情…', () async {
      final updated = await _api.updateDetails(
        skill.id,
        name: name,
        description: description,
        detail: detail,
        usageSteps: usageSteps,
      );
      privateSkills = privateSkills
          .map((item) => item.id == updated.id ? updated : item)
          .toList();
      publicSkills = publicSkills
          .map((item) => item.sourceSkillId == skill.id
              ? item.copyWith(
                  name: updated.name,
                  description: updated.description,
                  detail: updated.detail,
                  usageSteps: updated.usageSteps,
                )
              : item)
          .toList();
    }, successMessage: skill.shared ? '详情已保存，并同步到 Skill 公有池' : 'Skill 详情已保存');
  }

  Future<bool> pull(SkillItem skill) async {
    return _perform('正在拉取 Skill…', () async {
      final result = await _api.pullSkill(skill.id);
      privateSkills = <SkillItem>[
        result.skill,
        ...privateSkills.where((item) => item.id != result.skill.id),
      ];
      publicSkills = publicSkills
          .map(
            (item) => item.id == skill.id
                ? item.copyWith(
                    pulled: true,
                    pullCount: item.pullCount + (result.created ? 1 : 0),
                  )
                : item,
          )
          .toList();
    }, successMessage: '已拉取到你的私人空间');
  }

  Future<bool> share(SkillItem skill) async {
    return _perform('正在分享到公有池…', () async {
      await _api.shareSkill(skill.id);
      await refresh(showBusy: false);
    }, successMessage: 'Skill 已分享到公有池，个人备注保持私有');
  }

  Future<bool> unpublish(SkillItem skill) async {
    return _perform('正在从公有池下架…', () async {
      await _api.unpublishSkill(skill);
      await refresh(showBusy: false);
    }, successMessage: 'Skill 已从公有池下架，私人原件仍保留');
  }

  Future<bool> delete(SkillItem skill) async {
    return _perform('正在删除云端 Skill…', () async {
      await _api.deleteSkill(skill.id);
      privateSkills = privateSkills
          .where((item) => item.id != skill.id)
          .toList();
      publicSkills = publicSkills
          .where((item) => item.sourceSkillId != skill.id)
          .toList();
    }, successMessage: '云端 Skill 已删除');
  }

  Future<bool> submitFeedback(String kind, String content) async {
    return _perform('正在投递意见…', () async {
      await _api.submitFeedback(kind: kind, content: content);
    });
  }

  Future<FeedbackPage> loadFeedbackPage({int page = 1, int size = 6}) =>
      _api.feedbackPage(page: page, size: size);

  Future<bool> install(SkillItem skill, List<String> targets) async {
    return _perform(
      '正在下载、校验并安装…',
      () async {
        final content = await _api.downloadSkill(skill);
        await _installer.install(
          skill: skill,
          content: content,
          targets: targets,
        );
        activities.insert(
          0,
          LocalActivity(
            skillName: skill.name,
            action: LocalAction.install,
            targets: List<String>.from(targets),
            createdAt: DateTime.now(),
          ),
        );
        await _refreshLocalWorkspace(notify: false);
      },
      successMessage:
          '已安装到 ${targets.map((id) => toolLabels[id] ?? id).join('、')}',
    );
  }

  Future<bool> uninstall(SkillItem skill, List<String> targets) async {
    var removed = 0;
    final succeeded = await _perform('正在从本机卸载…', () async {
      removed = await _installer.uninstall(skill: skill, targets: targets);
      activities.insert(
        0,
        LocalActivity(
          skillName: skill.name,
          action: LocalAction.uninstall,
          targets: List<String>.from(targets),
          createdAt: DateTime.now(),
        ),
      );
      await _refreshLocalWorkspace(notify: false);
    });
    if (succeeded) {
      _show(removed == 0 ? '所选工具中没有这个 Skill' : '已永久删除 $removed 份本机副本');
    }
    return succeeded;
  }

  bool isInstalled(SkillItem skill, String target) =>
      _installer.isInstalled(skill, target);

  SkillItem? sourceSkillForLocal(LocalSkillItem local) {
    if (local.originSkillId != null) {
      for (final skill in privateSkills) {
        if (skill.id == local.originSkillId) return skill;
      }
    }
    for (final skill in privateSkills) {
      if (skillSlug(skill.name) == local.slug) return skill;
    }
    return null;
  }

  bool isFromMySkills(LocalSkillItem local) => sourceSkillForLocal(local) != null;

  Future<bool> uninstallLocalSkill(LocalSkillItem skill) async {
    var removed = false;
    final succeeded = await _perform('正在删除本机 Skill…', () async {
      removed = await _installer.uninstallLocalSkill(skill);
      activities.insert(
        0,
        LocalActivity(
          skillName: skill.name,
          action: LocalAction.uninstall,
          targets: <String>[skill.toolId],
          createdAt: DateTime.now(),
        ),
      );
      await _refreshLocalWorkspace(notify: false);
    });
    if (succeeded) _show(removed ? '已从本机永久删除 ${skill.name}' : '本机目录中已不存在该 Skill');
    return succeeded;
  }

  Future<void> refreshLocalWorkspace() async {
    await _perform('正在重新识别本机工作区…', () => _refreshLocalWorkspace(notify: false), successMessage: '本机工作区已刷新');
  }

  Future<bool> openLocalSkillFolder(LocalSkillItem skill) async {
    try {
      await _installer.openLocalSkillFolder(skill);
      _show('已打开 ${skill.name} 的本地文件夹');
      return true;
    } catch (error) {
      _show(error.toString(), error: true);
      return false;
    }
  }

  Future<String?> readLocalSkillManifest(LocalSkillItem skill) async {
    try {
      return await _installer.readLocalSkillManifest(skill);
    } catch (error) {
      _show(error.toString(), error: true);
      return null;
    }
  }

  Future<void> _refreshLocalWorkspace({bool notify = true}) async {
    tools = _installer.detectTools();
    final detectedIds = tools.where((tool) => tool.detected).map((tool) => tool.id);
    localSkills = await _installer.scanLocalSkills(toolIds: detectedIds);
    if (selectedLocalToolId == null || !tools.any((tool) => tool.detected && tool.id == selectedLocalToolId)) {
      selectedLocalToolId = detectedIds.isEmpty ? null : detectedIds.first;
    }
    if (notify) notifyListeners();
  }

  Future<bool> _perform(
    String label,
    Future<void> Function() operation, {
    String? successMessage,
  }) async {
    if (busy) return false;
    busy = true;
    busyLabel = label;
    notifyListeners();
    try {
      await operation();
      if (successMessage != null) _show(successMessage);
      return true;
    } catch (error) {
      _show(error.toString(), error: true);
      return false;
    } finally {
      busy = false;
      busyLabel = '';
      notifyListeners();
    }
  }

  void _show(String message, {bool error = false}) {
    feedback = FeedbackEvent(++_feedbackId, message, error: error);
    notifyListeners();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }
}
