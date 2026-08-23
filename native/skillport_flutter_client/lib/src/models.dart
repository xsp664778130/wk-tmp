enum LibraryMode { publicPool, privateSpace }

enum LocalAction { install, uninstall }

const skillCategories = <String>['全部技能', '编程技能', '测试技能', '排查技能', '日志技能'];

class SkillPortUser {
  const SkillPortUser({
    required this.id,
    required this.email,
    required this.displayName,
  });

  final String id;
  final String email;
  final String displayName;

  factory SkillPortUser.fromJson(Map<String, dynamic> json) => SkillPortUser(
    id: json['id']?.toString() ?? '',
    email: json['email']?.toString() ?? '',
    displayName: json['displayName']?.toString() ?? '',
  );
}

class SkillItem {
  const SkillItem({
    required this.id,
    required this.name,
    required this.description,
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
    category: normalizeCategory(json['category']),
    fileName: json['fileName']?.toString() ?? 'skill.zip',
    sizeBytes: _asInt(json['sizeBytes']),
    sha256: json['sha256']?.toString() ?? '',
    compatible: json['compatible'] is List
        ? (json['compatible'] as List).map((item) => item.toString()).toList()
        : const ['codex', 'qoder', 'openai'],
    author: json['author']?.toString() ?? 'SkillPort 用户',
    pullCount: _asInt(json['pullCount']),
    pulled: json['pulled'] == true,
    ownedByCurrentUser: json['ownedByCurrentUser'] == true,
    sourceSkillId: _nullableString(json['sourceSkillId']),
    avatarUrl: _nullableString(json['avatarUrl']),
    isPublic: true,
  );

  SkillItem copyWith({
    String? note,
    bool? shared,
    bool? pulled,
    int? pullCount,
  }) => SkillItem(
    id: id,
    name: name,
    description: description,
    category: category,
    fileName: fileName,
    sizeBytes: sizeBytes,
    sha256: sha256,
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

List<String> _compatibility(dynamic value) {
  final result = value
      ?.toString()
      .split(',')
      .map((item) => item.trim())
      .where((item) => item.isNotEmpty)
      .toList();
  return result == null || result.isEmpty
      ? const ['codex', 'qoder', 'openai']
      : result;
}
