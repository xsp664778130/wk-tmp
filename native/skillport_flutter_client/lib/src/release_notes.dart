import 'package:flutter/material.dart';

import 'app.dart';

const currentReleaseVersion = '1.0.9';
const currentReleaseDate = '2026-08-24';
const currentReleaseTitle = '上传兼容与草稿保护';
const currentReleaseChanges = <String>[
  '兼容压缩包内不同大小写的 SKILL.md，并增强失败路径提示。',
  '上传弹窗只能通过关闭按钮退出，避免误点遮罩丢失内容。',
  '自动恢复上次未提交的上传内容，并重新设计删除按钮。',
];

Future<void> showReleaseNotes(BuildContext context) => showDialog<void>(
  context: context,
  builder: (context) => const ReleaseNotesDialog(),
);

class VersionUpdateButton extends StatelessWidget {
  const VersionUpdateButton({super.key, this.compact = false});

  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Stack(
      clipBehavior: Clip.none,
      children: <Widget>[
        OutlinedButton.icon(
          onPressed: () => showReleaseNotes(context),
          style: OutlinedButton.styleFrom(
            foregroundColor: ink,
            side: const BorderSide(color: line),
            backgroundColor: Colors.white,
            minimumSize: compact ? const Size(44, 40) : const Size(154, 40),
            padding: compact
                ? const EdgeInsets.symmetric(horizontal: 11)
                : const EdgeInsets.symmetric(horizontal: 14),
          ),
          icon: const Icon(Icons.auto_awesome_rounded, size: 17, color: purple),
          label: compact
              ? const SizedBox.shrink()
              : const Text(
                  '版本更新  v$currentReleaseVersion',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
        ),
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
  const ReleaseNotesDialog({super.key});

  @override
  Widget build(BuildContext context) {
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
            child: const Icon(Icons.auto_awesome_rounded, color: purple),
          ),
          const SizedBox(width: 14),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'LATEST UPDATE',
                  style: TextStyle(
                    fontSize: 10,
                    color: purple,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 1.4,
                  ),
                ),
                SizedBox(height: 3),
                Text('版本更新', style: TextStyle(fontWeight: FontWeight.w900)),
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
            const Row(
              children: <Widget>[
                Text(
                  'v$currentReleaseVersion',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
                ),
                SizedBox(width: 9),
                Chip(
                  label: Text('当前版本'),
                  visualDensity: VisualDensity.compact,
                  labelStyle: TextStyle(
                    fontSize: 11,
                    color: purple,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Spacer(),
                Text(
                  currentReleaseDate,
                  style: TextStyle(fontSize: 12, color: muted),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Text(
              currentReleaseTitle,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900),
            ),
            const SizedBox(height: 13),
            ...currentReleaseChanges.map(
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
                color: const Color(0xFFF6F3FF),
                borderRadius: BorderRadius.circular(11),
              ),
              child: const Text(
                '以后每次发布都会在这里记录本次新增、优化和修复内容。',
                style: TextStyle(fontSize: 12, color: muted),
              ),
            ),
          ],
        ),
      ),
      actions: <Widget>[
        FilledButton(
          onPressed: () => Navigator.pop(context),
          style: FilledButton.styleFrom(backgroundColor: purple),
          child: const Text('我知道了'),
        ),
      ],
    );
  }
}
