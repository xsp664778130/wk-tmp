enum LibraryMode { publicPool, privateSpace, localWorkspace }

enum LocalAction { install, uninstall }

const skillCategories = <String>['全部技能', '编程技能', '测试技能', '排查技能', '日志技能'];
const defaultToolCompatibility = <String>[
  'codex',
  'qoder',
  'opencode',
  'claude',
  'cursor',
];

class SkillPortUser {
  const SkillPortUser({
    required this.id,
    required this.email,
    required this.displayName,
    this.passwordEnabled = true,
  });

  final String id;
  final String email;
  final String displayName;
  final bool passwordEnabled;

  factory SkillPortUser.fromJson(Map<String, dynamic> json) => SkillPortUser(
    id: json['id']?.toString() ?? '',
    email: json['email']?.toString() ?? '',
    displayName: json['displayName']?.toString() ?? '',
    passwordEnabled: json['passwordEnabled'] != false,
  );
}

class SkillItem {
  const SkillItem({
    required this.id,
    required this.name,
    required this.description,
    this.detail = '',
    this.usageSteps = const <String>[],
    required this.category,
    required this.fileName,
    required this.sizeBytes,
    required this.sha256,
    required this.compatible,
    this.note = '',
    this.author = '',
    this.pullCount = 0,
    this.shared = false,
    this.pulled = false,
    this.ownedByCurrentUser = false,
    this.sourceSkillId,
    this.avatarUrl,
    this.isPublic = false,
  });

  final String id;
  final String name;
  final String description;
  final String detail;
  final List<String> usageSteps;
  final String category;
  final String fileName;
  final int sizeBytes;
  final String sha256;
  final List<String> compatible;
  final String note;
  final String author;
  final int pullCount;
  final bool shared;
  final bool pulled;
  final bool ownedByCurrentUser;
  final String? sourceSkillId;
  final String? avatarUrl;
  final bool isPublic;

  factory SkillItem.fromPrivateJson(Map<String, dynamic> json) => SkillItem(
    id: json['id']?.toString() ?? '',
    name: json['name']?.toString() ?? '未命名 Skill',
    description: json['description']?.toString() ?? '',
    detail: json['detail']?.toString() ?? json['description']?.toString() ?? '',
    usageSteps: _stringList(json['usageSteps']),
    category: normalizeCategory(json['category']),
    fileName: json['fileName']?.toString() ?? 'skill.zip',
    sizeBytes: _asInt(json['sizeBytes']),
    sha256: json['sha256']?.toString() ?? '',
    note: json['note']?.toString() ?? '',
    compatible: _compatibility(json['toolCompatibility']),
    shared: json['shared'] == true,
    avatarUrl: _nullableString(json['avatarUrl']),
  );

  factory SkillItem.fromPublicJson(Map<String, dynamic> json) => SkillItem(
    id: json['id']?.toString() ?? '',
    name: json['name']?.toString() ?? '未命名 Skill',
    description: json['description']?.toString() ?? '',
    detail: json['detail']?.toString() ?? json['description']?.toString() ?? '',
    usageSteps: _stringList(json['usageSteps']),
    category: normalizeCategory(json['category']),
    fileName: json['fileName']?.toString() ?? 'skill.zip',
    sizeBytes: _asInt(json['sizeBytes']),
    sha256: json['sha256']?.toString() ?? '',
    compatible: _compatibility(json['compatible']),
    author: json['author']?.toString() ?? 'SkillPort 用户',
    pullCount: _asInt(json['pullCount']),
    pulled: json['pulled'] == true,
    ownedByCurrentUser: json['ownedByCurrentUser'] == true,
    sourceSkillId: _nullableString(json['sourceSkillId']),
    avatarUrl: _nullableString(json['avatarUrl']),
    isPublic: true,
  );

  SkillItem copyWith({
    String? name,
    String? description,
    String? detail,
    List<String>? usageSteps,
    String? category,
    String? fileName,
    int? sizeBytes,
    String? sha256,
    String? note,
    bool? shared,
    bool? pulled,
    int? pullCount,
  }) => SkillItem(
    id: id,
    name: name ?? this.name,
    description: description ?? this.description,
    detail: detail ?? this.detail,
    usageSteps: usageSteps ?? this.usageSteps,
    category: category ?? this.category,
    fileName: fileName ?? this.fileName,
    sizeBytes: sizeBytes ?? this.sizeBytes,
    sha256: sha256 ?? this.sha256,
    compatible: compatible,
    note: note ?? this.note,
    author: author,
    pullCount: pullCount ?? this.pullCount,
    shared: shared ?? this.shared,
    pulled: pulled ?? this.pulled,
    ownedByCurrentUser: ownedByCurrentUser,
    sourceSkillId: sourceSkillId,
    avatarUrl: avatarUrl,
    isPublic: isPublic,
  );
}

class ToolTarget {
  const ToolTarget({
    required this.id,
    required this.name,
    required this.directory,
    required this.detected,
  });

  final String id;
  final String name;
  final String directory;
  final bool detected;
}

class LocalSkillItem {
  const LocalSkillItem({
    required this.toolId,
    required this.slug,
    required this.name,
    required this.description,
    required this.directory,
    this.originSkillId,
  });

  final String toolId;
  final String slug;
  final String name;
  final String description;
  final String directory;
  final String? originSkillId;
}

class EnvironmentPropertiesView {
  const EnvironmentPropertiesView({
    required this.exists,
    required this.path,
    required this.values,
    required this.editable,
  });

  final bool exists;
  final String path;
  final Map<String, String> values;
  final bool editable;

  factory EnvironmentPropertiesView.fromJson(
    Map<String, dynamic> json, {
    required bool editable,
  }) {
    final source = json['values'];
    final values = <String, String>{};
    if (source is Map) {
      for (final entry in source.entries) {
        if (entry.key != null && entry.value is String) {
          values[entry.key.toString()] = entry.value as String;
        }
      }
    }
    final exists = json['exists'] == true;
    return EnvironmentPropertiesView(
      exists: exists,
      path: json['path']?.toString() ?? 'env.properties',
      values: Map<String, String>.unmodifiable(values),
      editable: exists && editable && json['editable'] != false,
    );
  }

  static const missing = EnvironmentPropertiesView(
    exists: false,
    path: 'env.properties',
    values: <String, String>{},
    editable: false,
  );
}

class LocalActivity {
  const LocalActivity({
    required this.skillName,
    required this.action,
    required this.targets,
    required this.createdAt,
  });

  final String skillName;
  final LocalAction action;
  final List<String> targets;
  final DateTime createdAt;
}

class PublicFeedbackItem {
  const PublicFeedbackItem({
    required this.id,
    required this.submitter,
    required this.kind,
    required this.content,
    required this.createdAt,
  });

  final String id;
  final String submitter;
  final String kind;
  final String content;
  final DateTime createdAt;

  factory PublicFeedbackItem.fromJson(Map<String, dynamic> json) =>
      PublicFeedbackItem(
        id: json['id']?.toString() ?? '',
        submitter: json['submitter']?.toString() ?? 'SkillPort 用户',
        kind: json['kind']?.toString() ?? '其他',
        content: json['content']?.toString() ?? '',
        createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? '') ??
            DateTime.fromMillisecondsSinceEpoch(0),
      );
}

class FeedbackPage {
  const FeedbackPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.hasPrevious,
    required this.hasNext,
  });

  final List<PublicFeedbackItem> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool hasPrevious;
  final bool hasNext;

  factory FeedbackPage.fromJson(Map<String, dynamic> json) => FeedbackPage(
    items: _list(json['items'])
        .map((item) => PublicFeedbackItem.fromJson(_object(item)))
        .toList(growable: false),
    page: _asInt(json['page']),
    size: _asInt(json['size']),
    totalElements: _asInt(json['totalElements']),
    totalPages: _asInt(json['totalPages']),
    hasPrevious: json['hasPrevious'] == true,
    hasNext: json['hasNext'] == true,
  );
}

String normalizeCategory(dynamic value) {
  const legacy = <String, String>{
    '编程开发': '编程技能',
    '测试工具': '测试技能',
    '排查工具': '排查技能',
    '日志报告': '日志技能',
  };
  final category = legacy[value?.toString().trim()] ?? value?.toString().trim();
  return skillCategories.contains(category) && category != '全部技能'
      ? category!
      : '编程技能';
}

int _asInt(dynamic value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;

String? _nullableString(dynamic value) {
  final text = value?.toString();
  return text == null || text.isEmpty ? null : text;
}

List<String> _stringList(dynamic value) => value is List
    ? value
          .map((item) => item.toString().trim())
          .where((item) => item.isNotEmpty)
          .toList(growable: false)
    : const <String>[];

List<String> _compatibility(dynamic value) {
  final values = value is List
      ? value.map((item) => item.toString())
      : value?.toString().split(',') ?? const <String>[];
  final requested = values
      .map((item) => item.trim())
      .where((item) => item.isNotEmpty)
      .toSet();
  if (requested.remove('openai')) {
    requested.addAll(const <String>['opencode', 'claude']);
  }
  final result = defaultToolCompatibility
      .where(requested.contains)
      .toList(growable: false);
  return result.isEmpty ? defaultToolCompatibility : result;
}

Map<String, dynamic> _object(dynamic value) =>
    value is Map<String, dynamic> ? value : <String, dynamic>{};

List<dynamic> _list(dynamic value) => value is List ? value : const <dynamic>[];
