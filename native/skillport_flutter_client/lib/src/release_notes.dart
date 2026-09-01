import 'package:flutter/material.dart';

import 'app.dart';
import 'client_release.dart';

const currentReleaseVersion = '1.0.37';
const currentReleaseDate = '2026-09-01';
const currentReleaseTitle = 'Skill 环境配置管理';
const currentReleaseChanges = <String>[
  '所有 Skill 卡片新增 env.properties 配置入口，公有池支持只读查看键值。',
  '我的 Skill 可直接修改现有配置值，并同步更新已分享公有池的下载文件。',
  '个人工作区可查看和编辑本机 env.properties，配置内容不会上传云端。',
];

class ReleaseNoteData {
  const ReleaseNoteData({
    required this.version,
    required this.date,
    required this.title,
    required this.changes,
  });

  final String version;
  final String date;
  final String title;
  final List<String> changes;
}

const bundledReleaseNotes = <ReleaseNoteData>[
  ReleaseNoteData(
    version: currentReleaseVersion,
    date: currentReleaseDate,
    title: currentReleaseTitle,
    changes: currentReleaseChanges,
  ),
  ReleaseNoteData(
    version: '1.0.36',
    date: '2026-08-31',
    title: '个人资料与密码找回',
    changes: <String>[
      '新增个人资料入口，可修改显示名称和当前登录密码。',
      '登录页新增忘记密码，可通过注册邮箱验证码设置新密码。',
      '验证码 10 分钟有效并限制尝试次数，密码变化后旧会话自动失效。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.35',
    date: '2026-08-28',
    title: 'Skill 压缩包独立替换',
    changes: <String>[
      '我的 Skill 详情新增“仅替换压缩包”，重新执行完整结构与大小校验。',
      '名称、描述、详细说明、使用步骤、分类、备注和头像全部保持不变。',
      '已分享 Skill 会同步更新公有池下载文件，其他用户已拉取的副本不受影响。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.34',
    date: '2026-08-28',
    title: 'macOS 压缩包安装兼容修复',
    changes: <String>[
      '桌面客户端改为根据压缩包中唯一有效的 SKILL.md 识别 Skill 根目录。',
      '自动忽略 __MACOSX、.DS_Store 与 AppleDouble 元数据，不再误报缺少 SKILL.md。',
      '兼容外层目录和大小写不同的 skill.md，同时继续执行完整的安装安全校验。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.33',
    date: '2026-08-28',
    title: '工作区卡片对齐优化',
    changes: <String>[
      '所有个人工作区卡片统一预留来源标识区域，不再因标识有无改变按钮位置。',
      '来自我的 Skill 与普通本机 Skill 的两个操作按钮保持在同一水平线上。',
      '网页端和桌面客户端同步采用固定底部操作区，卡片排列更加整齐。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.32',
    date: '2026-08-28',
    title: 'Windows 工作区按钮优化',
    changes: <String>[
      '个人工作区 Skill 卡片采用更合理的宽度，避免 Windows 下操作区过度拥挤。',
      '“打开文件夹”和“从本机卸载”固定为单行显示，不再出现难看的文字换行。',
      '两个操作按钮重新分配宽度并缩小图标和内边距，窄窗口下仍保持整齐。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.31',
    date: '2026-08-28',
    title: '本机 Skill 快速查看',
    changes: <String>[
      '个人工作区卡片新增“打开文件夹”，直接进入真实的本机 Skill 目录。',
      '卡片右上角新增三点菜单，可查看并滚动浏览完整 SKILL.md 内容。',
      '所有本机操作都会重新校验工具根目录和 Skill 路径，避免访问目录外文件。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.30',
    date: '2026-08-28',
    title: '环境高光主题',
    changes: <String>[
      '深夜紫、曜石黑与海湾蓝新增多层环境高光，页面更有空间感。',
      '新增极光青与暮霞玫两套高光主题，共支持 6 套可切换配色。',
      '主题菜单改为渐变光晕色卡预览，选择后仍会自动记住。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.29',
    date: '2026-08-28',
    title: 'AI 工具导航式工作区',
    changes: <String>[
      '左侧直接展示本机实际识别到的 AI 工具，并显示各工具中的 Skill 数量。',
      '点击 AI 工具后，主区域只展示该工具真实目录中的 Skill 卡片。',
      'SkillPort 安装项保留来源标识；删除其他本地 Skill 前会提示文件不可恢复。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.21',
    date: '2026-08-27',
    title: 'Skill 分类保存修复',
    changes: <String>[
      '我的 Skill 分类改为选择后立即自动保存，不再需要额外点击保存按钮。',
      '分类保存过程会显示明确状态，失败时自动恢复原分类。',
      '已分享 Skill 的分类保存成功后同步刷新公有池记录。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.20',
    date: '2026-08-27',
    title: 'Skill 详情与使用步骤',
    changes: <String>[
      '上传和编辑 Skill 时可维护完整详细说明与最多 20 个使用步骤。',
      'Skill 卡片新增详情摘要和步骤数量，点击后按编号查看完整使用方法。',
      '已分享 Skill 的名称、描述、详细说明和使用步骤会自动同步到公有池。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.19',
    date: '2026-08-27',
    title: '多主题配色',
    changes: <String>[
      '新增深夜紫、曜石黑、海湾蓝与晨雾白 4 套界面配色，可随时切换。',
      '默认采用参考图风格的深夜紫暗色界面，核心卡片、输入框与侧栏统一适配。',
      '主题选择保存在当前设备，客户端重启后仍会自动恢复。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.18',
    date: '2026-08-25',
    title: 'Skill 详情操作区焕新',
    changes: <String>[
      '客户端 Skill 详情页重新划分主操作、次级操作与危险操作，按钮统一尺寸和对齐。',
      '安装到本机作为唯一主按钮，卸载与公有池操作使用等宽次级按钮。',
      '分类改为选择后自动保存，保存备注回归内容区域，删除操作降低视觉干扰。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.17',
    date: '2026-08-25',
    title: '一键客户端自动更新',
    changes: <String>[
      '客户端发现云端新版本时，版本弹窗显示“立即更新到最新版”按钮。',
      '点击后在客户端内下载匹配当前系统的安装包，并实时显示下载进度。',
      '下载完成后自动启动 Windows 或 macOS 安装程序，不再跳转浏览器手动查找文件。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.16',
    date: '2026-08-25',
    title: 'Skill 分类同步编辑',
    changes: <String>[
      '我的 Skill 详情新增分类编辑，可以随时切换统一分类。',
      '已分享到公有池的 Skill 会在同一事务中同步修改公开分类。',
      '网页与桌面客户端都会立即刷新私人空间和公有池分类。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.15',
    date: '2026-08-25',
    title: '公开意见墙与分页',
    changes: <String>[
      '意见信箱升级为公开意见墙，所有用户都可以浏览大家提交的意见。',
      '每条意见显示提交人昵称、意见类型与准确提交时间。',
      '列表采用服务端 MySQL 分页，支持逐页浏览并保留传真提交动画。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.14',
    date: '2026-08-25',
    title: 'Cursor Skills 支持',
    changes: <String>[
      '本机 AI 工具新增 Cursor，并显示实际的 ~/.cursor/skills 目录。',
      '网页 Bridge 与桌面客户端均可把 Skill 安装或卸载到 Cursor。',
      'macOS 与 Windows 会根据 Cursor 应用或命令进行真实识别。',
    ],
  ),
];

class VersionUpdateButton extends StatefulWidget {
  const VersionUpdateButton({super.key, this.compact = false});

  final bool compact;

  @override
  State<VersionUpdateButton> createState() => _VersionUpdateButtonState();
}

class _VersionUpdateButtonState extends State<VersionUpdateButton> {
  late final ClientReleaseService _service;
  late final Future<void> _initialCheck;
  ClientReleaseInfo? _release;
  bool _checking = true;
  bool _checkFailed = false;

  bool get _updateAvailable =>
      _release != null &&
      isNewerVersion(_release!.version, currentReleaseVersion);

  @override
  void initState() {
    super.initState();
    _service = ClientReleaseService();
    _initialCheck = _checkForUpdate();
  }

  Future<void> _checkForUpdate() async {
    try {
      final release = await _service.fetchLatest();
      if (!mounted) return;
      setState(() {
        _release = release;
        _checking = false;
        _checkFailed = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _checking = false;
        _checkFailed = true;
      });
    }
  }

  Future<void> _showDetails() async {
    if (_checking) await _initialCheck;
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => ReleaseNotesDialog(
        release: _release,
        updateAvailable: _updateAvailable,
        checkFailed: _checkFailed,
        onUpdate: _release == null
            ? null
            : (onProgress) => _startUpdate(_release!, onProgress),
      ),
    );
  }

  Future<void> _startUpdate(
    ClientReleaseInfo release,
    ValueChanged<double> onProgress,
  ) async {
    await _service.downloadAndLaunch(release, onProgress: onProgress);
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('最新版安装程序已启动，请按系统提示完成更新。')));
  }

  @override
  void dispose() {
    _service.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final label = _updateAvailable
        ? '发现新版本  v${_release!.version}'
        : '版本更新  v$currentReleaseVersion';
    return Stack(
      clipBehavior: Clip.none,
      children: <Widget>[
        Tooltip(
          message: _updateAvailable ? '发现新版本，点击更新' : '查看版本更新',
          child: OutlinedButton.icon(
            onPressed: _showDetails,
            style: OutlinedButton.styleFrom(
              foregroundColor: _updateAvailable
                  ? scheme.primary
                  : scheme.onSurface,
              side: BorderSide(
                color: _updateAvailable
                    ? scheme.primary
                    : scheme.outlineVariant,
              ),
              backgroundColor: scheme.surface,
              minimumSize: widget.compact
                  ? const Size(44, 40)
                  : const Size(154, 40),
              padding: widget.compact
                  ? const EdgeInsets.symmetric(horizontal: 11)
                  : const EdgeInsets.symmetric(horizontal: 14),
            ),
            icon: Icon(
              _updateAvailable
                  ? Icons.system_update_alt_rounded
                  : Icons.auto_awesome_rounded,
              size: 17,
              color: purple,
            ),
            label: widget.compact
                ? const SizedBox.shrink()
                : Text(
                    label,
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
          ),
        ),
        if (_updateAvailable)
          const Positioned(
            top: -3,
            right: -3,
            child: IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Color(0xFFE34936),
                  shape: BoxShape.circle,
                  border: Border.fromBorderSide(
                    BorderSide(color: Colors.white, width: 2),
                  ),
                ),
                child: SizedBox(width: 11, height: 11),
              ),
            ),
          ),
      ],
    );
  }
}

class ReleaseNotesDialog extends StatefulWidget {
  const ReleaseNotesDialog({
    super.key,
    this.release,
    required this.updateAvailable,
    required this.checkFailed,
    this.onUpdate,
  });

  final ClientReleaseInfo? release;
  final bool updateAvailable;
  final bool checkFailed;
  final Future<void> Function(ValueChanged<double> onProgress)? onUpdate;

  @override
  State<ReleaseNotesDialog> createState() => _ReleaseNotesDialogState();
}

class _ReleaseNotesDialogState extends State<ReleaseNotesDialog> {
  final ScrollController _scrollController = ScrollController();
  bool _updating = false;
  double _updateProgress = 0;
  String? _updateError;

  List<ReleaseNoteData> get _shownReleases {
    final releases = <ReleaseNoteData>[];
    if (widget.updateAvailable && widget.release != null) {
      releases.add(
        ReleaseNoteData(
          version: widget.release!.version,
          date: widget.release!.date,
          title: widget.release!.title,
          changes: widget.release!.changes,
        ),
      );
    }
    for (final release in bundledReleaseNotes) {
      if (!releases.any((item) => item.version == release.version)) {
        releases.add(release);
      }
    }
    return releases.take(5).toList(growable: false);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _runUpdate() async {
    final update = widget.onUpdate;
    if (update == null || _updating) return;
    setState(() {
      _updating = true;
      _updateProgress = 0;
      _updateError = null;
    });
    try {
      await update((progress) {
        if (!mounted) return;
        setState(() => _updateProgress = progress.clamp(0, 1).toDouble());
      });
      if (mounted) Navigator.pop(context);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _updating = false;
        _updateError = '更新失败，请检查网络后重试。';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final contentHeight = (MediaQuery.sizeOf(context).height * 0.58)
        .clamp(300.0, 520.0)
        .toDouble();
    return AlertDialog(
      constraints: const BoxConstraints(maxWidth: 560),
      titlePadding: const EdgeInsets.fromLTRB(28, 25, 18, 0),
      contentPadding: const EdgeInsets.fromLTRB(28, 22, 28, 12),
      actionsPadding: const EdgeInsets.fromLTRB(20, 0, 20, 18),
      title: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: scheme.primaryContainer,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              widget.updateAvailable
                  ? Icons.system_update_alt_rounded
                  : Icons.auto_awesome_rounded,
              color: scheme.primary,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  widget.updateAvailable ? 'UPDATE AVAILABLE' : 'LATEST UPDATE',
                  style: TextStyle(
                    fontSize: 10,
                    color: scheme.primary,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 1.4,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  widget.updateAvailable ? '发现新版本' : '版本更新',
                  style: const TextStyle(fontWeight: FontWeight.w900),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: _updating ? null : () => Navigator.pop(context),
            tooltip: '关闭',
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
      content: SizedBox(
        height: contentHeight,
        width: double.maxFinite,
        child: Scrollbar(
          controller: _scrollController,
          thumbVisibility: true,
          trackVisibility: true,
          thickness: 7,
          radius: const Radius.circular(8),
          child: ListView.separated(
            controller: _scrollController,
            padding: const EdgeInsets.only(right: 18),
            itemCount: _shownReleases.length + 1,
            separatorBuilder: (_, _) => const SizedBox(height: 14),
            itemBuilder: (context, index) {
              if (index == _shownReleases.length) {
                return _ReleaseStatusNotice(
                  checkFailed: widget.checkFailed,
                  updateAvailable: widget.updateAvailable,
                );
              }
              final release = _shownReleases[index];
              return _ReleaseNoteCard(
                release: release,
                status: widget.updateAvailable && index == 0
                    ? '可以更新'
                    : release.version == currentReleaseVersion
                    ? '当前版本'
                    : '历史版本',
                highlighted: index == 0,
              );
            },
          ),
        ),
      ),
      actions: <Widget>[
        if (_updateError != null)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: Text(
              _updateError!,
              style: const TextStyle(
                color: Color(0xFFC74331),
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        if (widget.updateAvailable && widget.onUpdate != null)
          FilledButton.icon(
            onPressed: _updating ? null : _runUpdate,
            style: FilledButton.styleFrom(backgroundColor: scheme.primary),
            icon: _updating
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Icon(Icons.system_update_alt_rounded),
            label: Text(
              _updating
                  ? '正在下载 ${(_updateProgress * 100).round()}%'
                  : '立即更新到 v${widget.release!.version}',
            ),
          )
        else
          FilledButton(
            onPressed: () => Navigator.pop(context),
            style: FilledButton.styleFrom(backgroundColor: scheme.primary),
            child: const Text('我知道了'),
          ),
      ],
    );
  }
}

class _ReleaseNoteCard extends StatelessWidget {
  const _ReleaseNoteCard({
    required this.release,
    required this.status,
    required this.highlighted,
  });

  final ReleaseNoteData release;
  final String status;
  final bool highlighted;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: highlighted
            ? scheme.primaryContainer.withValues(alpha: .28)
            : scheme.surface,
        border: Border.all(
          color: highlighted
              ? scheme.primary.withValues(alpha: .55)
              : scheme.outlineVariant,
        ),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                'v${release.version}',
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(width: 9),
              Chip(
                label: Text(status),
                visualDensity: VisualDensity.compact,
                labelStyle: TextStyle(
                  fontSize: 11,
                  color: status == '历史版本'
                      ? scheme.onSurfaceVariant
                      : scheme.primary,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const Spacer(),
              Text(
                release.date,
                style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            release.title,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 10),
          ...release.changes.map(
            (change) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Padding(
                    padding: const EdgeInsets.only(top: 7),
                    child: Icon(Icons.circle, size: 6, color: scheme.primary),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      change,
                      style: TextStyle(
                        height: 1.45,
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ReleaseStatusNotice extends StatelessWidget {
  const _ReleaseStatusNotice({
    required this.checkFailed,
    required this.updateAvailable,
  });

  final bool checkFailed;
  final bool updateAvailable;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: checkFailed
            ? scheme.errorContainer
            : scheme.primaryContainer.withValues(alpha: .32),
        borderRadius: BorderRadius.circular(11),
      ),
      child: Text(
        checkFailed
            ? '暂时无法连接更新服务，当前显示客户端内置的最近 5 个版本。'
            : updateAvailable
            ? '点击“立即更新”会下载适合这台电脑的安装包并自动启动；macOS 会显示系统安装确认。'
            : '当前已是最新版本；向下滚动可查看最近 5 个版本。',
        style: TextStyle(fontSize: 12, color: scheme.onSurfaceVariant),
      ),
    );
  }
}
