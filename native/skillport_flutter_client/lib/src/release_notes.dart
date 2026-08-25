import 'package:flutter/material.dart';

import 'app.dart';
import 'client_release.dart';

const currentReleaseVersion = '1.0.10';
const currentReleaseDate = '2026-08-25';
const currentReleaseTitle = '客户端在线更新';
const currentReleaseChanges = <String>[
  '客户端自动检查云端最新版本，并准确比较当前版本。',
  '发现新版本时显示更新提示、版本号和红点提醒。',
  '更新弹窗按当前系统提供 macOS 或 Windows 的立即更新按钮。',
  '已是最新版时明确显示当前版本，不再持续显示误导性的红点。',
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

class ReleaseNotesDialog extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final shownVersion = updateAvailable
        ? release!.version
        : currentReleaseVersion;
    final shownDate = updateAvailable ? release!.date : currentReleaseDate;
    final shownTitle = updateAvailable ? release!.title : currentReleaseTitle;
    final shownChanges = updateAvailable && release!.changes.isNotEmpty
        ? release!.changes
        : currentReleaseChanges;
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
              updateAvailable
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
                  updateAvailable ? 'UPDATE AVAILABLE' : 'LATEST UPDATE',
                  style: const TextStyle(
                    fontSize: 10,
                    color: purple,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 1.4,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  updateAvailable ? '发现新版本' : '版本更新',
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
      content: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Text(
                  'v$shownVersion',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(width: 9),
                Chip(
                  label: Text(updateAvailable ? '可以更新' : '当前版本'),
                  visualDensity: VisualDensity.compact,
                  labelStyle: const TextStyle(
                    fontSize: 11,
                    color: purple,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const Spacer(),
                Text(
                  shownDate,
                  style: const TextStyle(fontSize: 12, color: muted),
                ),
              ],
            ),
            if (updateAvailable) ...<Widget>[
              const SizedBox(height: 4),
              const Text(
                '当前安装版本：v$currentReleaseVersion',
                style: TextStyle(fontSize: 12, color: muted),
              ),
            ],
            const SizedBox(height: 12),
            Text(
              shownTitle,
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900),
            ),
            const SizedBox(height: 13),
            ...shownChanges.map(
              (change) => Padding(
                padding: const EdgeInsets.only(bottom: 10),
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
                        style: const TextStyle(height: 1.5, color: muted),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 8),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(13),
              decoration: BoxDecoration(
                color: checkFailed
                    ? const Color(0xFFFFF5E7)
                    : const Color(0xFFF6F3FF),
                borderRadius: BorderRadius.circular(11),
              ),
              child: Text(
                checkFailed
                    ? '暂时无法连接更新服务，当前显示客户端内置的版本信息。'
                    : updateAvailable
                    ? '点击“立即更新”会自动下载适合这台电脑的安装包。'
                    : '当前已是最新版本，后续有更新时这里会主动提示。',
                style: const TextStyle(fontSize: 12, color: muted),
              ),
            ),
          ],
        ),
      ),
      actions: <Widget>[
        if (updateAvailable && onUpdate != null)
          FilledButton.icon(
            onPressed: onUpdate,
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
