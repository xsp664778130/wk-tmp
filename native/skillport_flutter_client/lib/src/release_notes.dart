import 'package:flutter/material.dart';

import 'app.dart';
import 'client_release.dart';

const currentReleaseVersion = '1.0.18';
const currentReleaseDate = '2026-08-25';
const currentReleaseTitle = 'Skill 详情操作区焕新';
const currentReleaseChanges = <String>[
  '客户端 Skill 详情页重新划分主操作、次级操作与危险操作，按钮统一尺寸和对齐。',
  '安装到本机作为唯一主按钮，卸载与公有池操作使用等宽次级按钮。',
  '分类改为选择后自动保存，保存备注回归内容区域，删除操作降低视觉干扰。',
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
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('最新版安装程序已启动，请按系统提示完成更新。')),
    );
  }

  @override
  void dispose() {
    _service.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
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
              foregroundColor: _updateAvailable ? purple : ink,
              side: BorderSide(color: _updateAvailable ? purple : line),
              backgroundColor: Colors.white,
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
        setState(
          () => _updateProgress = progress.clamp(0, 1).toDouble(),
        );
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
              color: const Color(0xFFECE7FF),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              widget.updateAvailable
                  ? Icons.system_update_alt_rounded
                  : Icons.auto_awesome_rounded,
              color: purple,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  widget.updateAvailable ? 'UPDATE AVAILABLE' : 'LATEST UPDATE',
                  style: const TextStyle(
                    fontSize: 10,
                    color: purple,
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
            style: FilledButton.styleFrom(backgroundColor: purple),
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
            style: FilledButton.styleFrom(backgroundColor: purple),
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
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: highlighted ? const Color(0xFFF8F6FF) : Colors.white,
        border: Border.all(color: highlighted ? const Color(0xFFD8CFFF) : line),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Text(
                'v${release.version}',
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
              ),
              const SizedBox(width: 9),
              Chip(
                label: Text(status),
                visualDensity: VisualDensity.compact,
                labelStyle: TextStyle(
                  fontSize: 11,
                  color: status == '历史版本' ? muted : purple,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const Spacer(),
              Text(release.date, style: const TextStyle(fontSize: 12, color: muted)),
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
                  const Padding(
                    padding: EdgeInsets.only(top: 7),
                    child: Icon(Icons.circle, size: 6, color: purple),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      change,
                      style: const TextStyle(height: 1.45, color: muted),
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
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: checkFailed ? const Color(0xFFFFF5E7) : const Color(0xFFF6F3FF),
        borderRadius: BorderRadius.circular(11),
      ),
      child: Text(
        checkFailed
            ? '暂时无法连接更新服务，当前显示客户端内置的最近 5 个版本。'
            : updateAvailable
            ? '点击“立即更新”会下载适合这台电脑的安装包并自动启动；macOS 会显示系统安装确认。'
            : '当前已是最新版本；向下滚动可查看最近 5 个版本。',
        style: const TextStyle(fontSize: 12, color: muted),
      ),
    );
  }
}
