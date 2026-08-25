import 'package:flutter/material.dart';

import 'app.dart';
import 'client_release.dart';

const currentReleaseVersion = '1.0.14';
const currentReleaseDate = '2026-08-25';
const currentReleaseTitle = 'Cursor Skills 支持';
const currentReleaseChanges = <String>[
  '本机 AI 工具新增 Cursor，并显示实际的 ~/.cursor/skills 目录。',
  '网页 Bridge 与桌面客户端均可把 Skill 安装或卸载到 Cursor。',
  'macOS 与 Windows 会根据 Cursor 应用或命令进行真实识别。',
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
    version: '1.0.13',
    date: '2026-08-25',
    title: '意见信箱与传真动画',
    changes: <String>[
      '网页与桌面客户端新增账号专属的意见信箱入口。',
      '提交时播放纸张扫描、信号传输和送达回执的传真动画。',
      '意见安全写入 MySQL，并与提交用户账号绑定。',
    ],
  ),
  ReleaseNoteData(
    version: currentReleaseVersion,
    date: currentReleaseDate,
    title: currentReleaseTitle,
    changes: currentReleaseChanges,
  ),
  ReleaseNoteData(
    version: '1.0.12',
    date: '2026-08-25',
    title: '最近 5 个版本记录',
    changes: <String>[
      '版本更新弹窗新增固定滚动区域和始终可见的滚动条。',
      '按发布时间从新到旧展示最近 5 个版本的完整更新内容。',
      '发现云端新版本时自动置顶，并保留最近历史记录。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.11',
    date: '2026-08-25',
    title: '本机 Skill 目录可见',
    changes: <String>[
      '已识别的 AI 工具卡片直接显示实际本机 Skills 目录。',
      '目录路径使用独立文件夹样式，长路径自动折行并保留完整提示。',
      '未检测到的工具不展示目录，避免把预设路径误认为已安装目录。',
    ],
  ),
  ReleaseNoteData(
    version: '1.0.10',
    date: '2026-08-25',
    title: '客户端在线更新',
    changes: <String>[
      '客户端自动检查云端最新版本，并准确比较当前版本。',
      '发现新版本时显示更新提示、版本号和红点提醒。',
      '更新弹窗按当前系统提供 macOS 或 Windows 的立即更新按钮。',
      '已是最新版时明确显示当前版本，不再持续显示误导性的红点。',
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
      builder: (dialogContext) => ReleaseNotesDialog(
        release: _release,
        updateAvailable: _updateAvailable,
        checkFailed: _checkFailed,
        onUpdate: _release == null
            ? null
            : () => _startUpdate(dialogContext, _release!),
      ),
    );
  }

  Future<void> _startUpdate(
    BuildContext dialogContext,
    ClientReleaseInfo release,
  ) async {
    try {
      await _service.openInstaller(release);
      if (!mounted || !dialogContext.mounted) return;
      Navigator.pop(dialogContext);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('已打开最新版安装包下载，下载完成后运行即可更新。')),
      );
    } catch (_) {
      if (!dialogContext.mounted) return;
      ScaffoldMessenger.of(dialogContext).showSnackBar(
        const SnackBar(content: Text('暂时无法打开更新下载，请稍后重试。')),
      );
    }
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
  final Future<void> Function()? onUpdate;

  @override
  State<ReleaseNotesDialog> createState() => _ReleaseNotesDialogState();
}

class _ReleaseNotesDialogState extends State<ReleaseNotesDialog> {
  final ScrollController _scrollController = ScrollController();

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
            onPressed: () => Navigator.pop(context),
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
        if (widget.updateAvailable && widget.onUpdate != null)
          FilledButton.icon(
            onPressed: widget.onUpdate,
            style: FilledButton.styleFrom(backgroundColor: purple),
            icon: const Icon(Icons.download_rounded),
            label: const Text('立即更新'),
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
            ? '点击“立即更新”会自动下载适合这台电脑的安装包。'
            : '当前已是最新版本；向下滚动可查看最近 5 个版本。',
        style: const TextStyle(fontSize: 12, color: muted),
      ),
    );
  }
}
