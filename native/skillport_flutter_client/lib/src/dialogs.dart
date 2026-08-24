import 'package:desktop_drop/desktop_drop.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';

import 'app.dart';
import 'app_controller.dart';
import 'local_installer.dart';
import 'models.dart';

Future<void> showSkillDetail(
  BuildContext context,
  AppController controller,
  SkillItem skill,
) => showDialog<void>(
  context: context,
  builder: (_) => SkillDetailDialog(controller: controller, skill: skill),
);

Future<void> showUploadDialog(
  BuildContext context,
  AppController controller, {
  XFile? initialFile,
}) => showDialog<void>(
  context: context,
  builder: (_) =>
      UploadDialog(controller: controller, initialFile: initialFile),
);

Future<void> showInstallDialog(
  BuildContext context,
  AppController controller,
  SkillItem skill,
  LocalAction action,
) => showDialog<void>(
  context: context,
  builder: (_) =>
      InstallDialog(controller: controller, skill: skill, action: action),
);

class SkillDetailDialog extends StatefulWidget {
  const SkillDetailDialog({
    super.key,
    required this.controller,
    required this.skill,
  });

  final AppController controller;
  final SkillItem skill;

  @override
  State<SkillDetailDialog> createState() => _SkillDetailDialogState();
}

class _SkillDetailDialogState extends State<SkillDetailDialog> {
  late final TextEditingController _note = TextEditingController(
    text: widget.skill.note,
  );

  @override
  void dispose() {
    _note.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final skill = widget.skill;
    return AlertDialog(
      constraints: const BoxConstraints(maxWidth: 610),
      title: Row(
        children: <Widget>[
          _DialogAvatar(skill: skill),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  skill.name,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                Text(
                  skill.category,
                  style: const TextStyle(fontSize: 12, color: muted),
                ),
              ],
            ),
          ),
        ],
      ),
      content: SizedBox(
        width: 560,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                skill.description.isEmpty ? '暂无描述' : skill.description,
                style: const TextStyle(color: muted, height: 1.6),
              ),
              const SizedBox(height: 18),
              Wrap(
                spacing: 7,
                children: skill.compatible
                    .map((id) => Chip(label: Text(toolLabels[id] ?? id)))
                    .toList(),
              ),
              if (skill.isPublic) ...<Widget>[
                const SizedBox(height: 15),
                Container(
                  padding: const EdgeInsets.all(13),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF2EEFF),
                    borderRadius: BorderRadius.circular(11),
                  ),
                  child: const Row(
                    children: <Widget>[
                      Icon(Icons.lock_outline_rounded, color: purple),
                      SizedBox(width: 10),
                      Expanded(child: Text('拉取后生成你的私人副本，发布者看不到你的备注和修改。')),
                    ],
                  ),
                ),
              ] else ...<Widget>[
                const SizedBox(height: 15),
                TextField(
                  controller: _note,
                  maxLines: 4,
                  maxLength: 2000,
                  decoration: const InputDecoration(
                    labelText: '我的备注（仅自己可见）',
                    alignLabelWithHint: true,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: skill.isPublic
          ? _publicActions(context, skill)
          : _privateActions(context, skill),
    );
  }

  List<Widget> _publicActions(BuildContext context, SkillItem skill) =>
      <Widget>[
        SizedBox(
          width: 560,
          child: Row(
            children: <Widget>[
              if (skill.ownedByCurrentUser)
                OutlinedButton.icon(
                  onPressed: widget.controller.busy
                      ? null
                      : () async {
                          if (await widget.controller.unpublish(skill) &&
                              context.mounted) {
                            Navigator.pop(context);
                          }
                        },
                  icon: const Icon(Icons.public_off_outlined, size: 17),
                  label: const Text('从公有池下架'),
                ),
              const Spacer(),
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('关闭'),
              ),
              const SizedBox(width: 8),
              FilledButton.icon(
                onPressed: skill.pulled || widget.controller.busy
                    ? null
                    : () async {
                        if (await widget.controller.pull(skill) &&
                            context.mounted) {
                          Navigator.pop(context);
                        }
                      },
                style: FilledButton.styleFrom(backgroundColor: purple),
                icon: Icon(
                  skill.pulled ? Icons.check_rounded : Icons.south_rounded,
                  size: 17,
                ),
                label: Text(skill.pulled ? '已在我的空间' : '拉取到我的空间'),
              ),
            ],
          ),
        ),
      ];

  List<Widget> _privateActions(BuildContext context, SkillItem skill) =>
      <Widget>[
        SizedBox(
          width: 560,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Row(
                children: <Widget>[
                  TextButton.icon(
                    onPressed: widget.controller.busy
                        ? null
                        : () => confirmDeleteSkill(
                            context,
                            widget.controller,
                            skill,
                            closeParentOnSuccess: true,
                          ),
                    style: TextButton.styleFrom(
                      foregroundColor: const Color(0xFFB04435),
                    ),
                    icon: const Icon(Icons.delete_outline_rounded, size: 17),
                    label: const Text('删除云端 Skill'),
                  ),
                  const SizedBox(width: 6),
                  OutlinedButton.icon(
                    onPressed: widget.controller.busy
                        ? null
                        : () => showInstallDialog(
                            context,
                            widget.controller,
                            skill,
                            LocalAction.uninstall,
                          ),
                    icon: const Icon(Icons.remove_circle_outline, size: 17),
                    label: const Text('从本机卸载'),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: <Widget>[
                  TextButton.icon(
                    onPressed: widget.controller.busy
                        ? null
                        : () => widget.controller.updateNote(skill, _note.text),
                    icon: const Icon(Icons.save_outlined, size: 17),
                    label: const Text('保存备注'),
                  ),
                  const SizedBox(width: 6),
                  OutlinedButton.icon(
                    onPressed: widget.controller.busy
                        ? null
                        : skill.shared
                        ? () => widget.controller.unpublish(skill)
                        : () => widget.controller.share(skill),
                    icon: Icon(
                      skill.shared
                          ? Icons.public_off_outlined
                          : Icons.ios_share_rounded,
                      size: 17,
                    ),
                    label: Text(skill.shared ? '从公有池下架' : '分享到公有池'),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: widget.controller.busy
                          ? null
                          : () => showInstallDialog(
                              context,
                              widget.controller,
                              skill,
                              LocalAction.install,
                            ),
                      style: FilledButton.styleFrom(backgroundColor: purple),
                      icon: const Icon(Icons.download_done_rounded, size: 17),
                      label: const Text('安装到本机'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ];
}

class UploadDialog extends StatefulWidget {
  const UploadDialog({super.key, required this.controller, this.initialFile});

  final AppController controller;
  final XFile? initialFile;

  @override
  State<UploadDialog> createState() => _UploadDialogState();
}

class _UploadDialogState extends State<UploadDialog> {
  XFile? _file;
  XFile? _avatar;
  String _category = '编程技能';
  final _name = TextEditingController();
  final _description = TextEditingController();
  final _note = TextEditingController();
  bool _dragging = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _file = widget.initialFile;
    _name.text = _nameFromFile(_file);
  }

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _pickSkill() async {
    final file = await openFile(
      acceptedTypeGroups: const <XTypeGroup>[
        XTypeGroup(label: 'Skill', extensions: <String>['zip', 'skill', 'md']),
      ],
    );
    if (file != null) {
      setState(() {
        _file = file;
        if (_name.text.trim().isEmpty) _name.text = _nameFromFile(file);
        _error = null;
      });
    }
  }

  Future<void> _pickAvatar() async {
    final file = await openFile(
      acceptedTypeGroups: const <XTypeGroup>[
        XTypeGroup(
          label: '图片',
          extensions: <String>['png', 'jpg', 'jpeg', 'webp', 'gif'],
        ),
      ],
    );
    if (file != null) setState(() => _avatar = file);
  }

  Future<void> _submit() async {
    if (_file == null) return;
    if (!RegExp(
      r'\.(zip|skill|md)$',
      caseSensitive: false,
    ).hasMatch(_file!.name)) {
      setState(() => _error = '仅支持 .zip、.skill 或 SKILL.md。');
      return;
    }
    if (_name.text.trim().isEmpty || _description.text.trim().isEmpty) {
      setState(() => _error = '请填写 Skill 名称和描述后再上传。');
      return;
    }
    final success = await widget.controller.upload(
      filePath: _file!.path,
      name: _name.text.trim(),
      description: _description.text.trim(),
      category: _category,
      note: _note.text,
      avatarPath: _avatar?.path,
    );
    if (success && mounted) {
      Navigator.pop(context);
    } else if (mounted) {
      setState(() => _error = widget.controller.feedback?.message ?? '上传失败');
    }
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    constraints: const BoxConstraints(maxWidth: 600),
    title: const Text(
      '上传你的 Skill',
      style: TextStyle(fontWeight: FontWeight.w900),
    ),
    content: SizedBox(
      width: 550,
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            const Text(
              '名称、描述和分类由你设置；服务器仍会检查目录和 SKILL.md。',
              style: TextStyle(color: muted),
            ),
            const SizedBox(height: 15),
            DropTarget(
              onDragEntered: (_) => setState(() => _dragging = true),
              onDragExited: (_) => setState(() => _dragging = false),
              onDragDone: (detail) => setState(() {
                _dragging = false;
                if (detail.files.isNotEmpty) {
                  _file = detail.files.first;
                  if (_name.text.trim().isEmpty) {
                    _name.text = _nameFromFile(_file);
                  }
                }
              }),
              child: InkWell(
                onTap: _pickSkill,
                borderRadius: BorderRadius.circular(14),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 150),
                  height: 118,
                  decoration: BoxDecoration(
                    color: _dragging
                        ? const Color(0xFFEDE7FF)
                        : const Color(0xFFF8F6FE),
                    border: Border.all(
                      color: _dragging ? purple : const Color(0xFFCFC9DD),
                      width: _dragging ? 2 : 1,
                    ),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Icon(
                        _file == null
                            ? Icons.file_upload_outlined
                            : Icons.check_circle_rounded,
                        color: purple,
                        size: 30,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _file?.name ?? '拖动或点击选择 Skill 文件',
                        style: const TextStyle(fontWeight: FontWeight.w800),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        '.zip、.skill 或 SKILL.md，最大 25MB',
                        style: TextStyle(fontSize: 11, color: muted),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _name,
              maxLength: 160,
              decoration: const InputDecoration(
                labelText: 'Skill 名称（必填）',
                helperText: '分享到公有池时同步使用此名称',
              ),
              onChanged: (_) => setState(() => _error = null),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _description,
              minLines: 2,
              maxLines: 4,
              maxLength: 2000,
              decoration: const InputDecoration(
                labelText: 'Skill 描述（必填）',
                helperText: '分享到公有池时同步使用此描述',
                alignLabelWithHint: true,
              ),
              onChanged: (_) => setState(() => _error = null),
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: _category,
              decoration: const InputDecoration(labelText: '分类（必选）'),
              items: skillCategories
                  .skip(1)
                  .map(
                    (item) => DropdownMenuItem(value: item, child: Text(item)),
                  )
                  .toList(),
              onChanged: (value) =>
                  setState(() => _category = value ?? _category),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: _note,
              maxLines: 3,
              maxLength: 2000,
              decoration: const InputDecoration(
                labelText: '个人备注（可选）',
                alignLabelWithHint: true,
              ),
            ),
            OutlinedButton.icon(
              onPressed: _pickAvatar,
              icon: const Icon(Icons.image_outlined),
              label: Text(_avatar == null ? '选择 Skill 头像（可选）' : _avatar!.name),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF9E6),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Text(
                '标准结构：skill-name/SKILL.md；scripts、references、assets 为可选目录。',
                style: TextStyle(fontSize: 12, color: Color(0xFF776629)),
              ),
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(
                  _error!,
                  style: const TextStyle(color: Color(0xFFB04435)),
                ),
              ),
          ],
        ),
      ),
    ),
    actions: <Widget>[
      TextButton(
        onPressed: widget.controller.busy ? null : () => Navigator.pop(context),
        child: const Text('取消'),
      ),
      FilledButton(
        onPressed: _file == null || widget.controller.busy ? null : _submit,
        child: Text(
          widget.controller.busy ? widget.controller.busyLabel : '检查并保存',
        ),
      ),
    ],
  );
}

String _nameFromFile(XFile? file) {
  if (file == null) return '';
  return file.name
      .replaceFirst(RegExp(r'\.(zip|skill|md)$', caseSensitive: false), '')
      .trim();
}

class InstallDialog extends StatefulWidget {
  const InstallDialog({
    super.key,
    required this.controller,
    required this.skill,
    required this.action,
  });

  final AppController controller;
  final SkillItem skill;
  final LocalAction action;

  @override
  State<InstallDialog> createState() => _InstallDialogState();
}

class _InstallDialogState extends State<InstallDialog> {
  late final Set<String> _targets = <String>{
    ...widget.skill.compatible
        .where(
          (id) => widget.controller.tools.any(
            (tool) => tool.id == id && tool.detected,
          ),
        )
        .take(1),
  };
  String? _error;

  @override
  Widget build(BuildContext context) {
    final removing = widget.action == LocalAction.uninstall;
    return AlertDialog(
      constraints: const BoxConstraints(maxWidth: 580),
      title: Text(
        '${removing ? '从本机卸载' : '安装到本机'} ${widget.skill.name}',
        style: const TextStyle(fontWeight: FontWeight.w900),
      ),
      content: SizedBox(
        width: 530,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Text(
              removing
                  ? '只删除选择工具中的本机副本，不删除云端 Skill。'
                  : '客户端下载云端原件、校验 SHA-256 后直接写入本机工具目录。',
              style: const TextStyle(color: muted),
            ),
            const SizedBox(height: 15),
            ...widget.controller.tools
                .where((tool) => widget.skill.compatible.contains(tool.id))
                .map((tool) {
                  final selected = _targets.contains(tool.id);
                  final installed = widget.controller.isInstalled(
                    widget.skill,
                    tool.id,
                  );
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: CheckboxListTile(
                      value: selected,
                      onChanged: widget.controller.busy
                          ? null
                          : (value) => setState(
                              () => value == true
                                  ? _targets.add(tool.id)
                                  : _targets.remove(tool.id),
                            ),
                      title: Text(
                        tool.name,
                        style: const TextStyle(fontWeight: FontWeight.w800),
                      ),
                      subtitle: Text(
                        '${tool.directory}\n${installed
                            ? '已安装这个 Skill'
                            : tool.detected
                            ? '已识别工具'
                            : '未检测到工具目录，安装时会创建'}',
                        style: const TextStyle(fontSize: 11),
                      ),
                      secondary: CircleAvatar(
                        child: Text(
                          tool.id == 'openai'
                              ? 'AI'
                              : tool.id == 'codex'
                              ? 'CX'
                              : 'Q',
                          style: const TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                      controlAffinity: ListTileControlAffinity.trailing,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: BorderSide(color: selected ? purple : line),
                      ),
                    ),
                  );
                }),
            if (removing) ...<Widget>[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFECE8),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Text(
                  '本机副本会被永久删除，不保留备份；需要时可以重新安装。',
                  style: TextStyle(
                    color: Color(0xFF9D4334),
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(
                  _error!,
                  style: const TextStyle(color: Color(0xFFB04435)),
                ),
              ),
          ],
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: widget.controller.busy
              ? null
              : () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        FilledButton(
          onPressed: _targets.isEmpty || widget.controller.busy
              ? null
              : () async {
                  final success = removing
                      ? await widget.controller.uninstall(
                          widget.skill,
                          _targets.toList(),
                        )
                      : await widget.controller.install(
                          widget.skill,
                          _targets.toList(),
                        );
                  if (success && context.mounted) Navigator.pop(context);
                  if (!success && mounted) {
                    setState(
                      () => _error = widget.controller.feedback?.message,
                    );
                  }
                },
          style: removing
              ? FilledButton.styleFrom(backgroundColor: const Color(0xFFAE4B3B))
              : null,
          child: Text(
            widget.controller.busy
                ? widget.controller.busyLabel
                : removing
                ? '确认永久卸载'
                : '下载并安装',
          ),
        ),
      ],
    );
  }
}

Future<void> confirmDeleteSkill(
  BuildContext context,
  AppController controller,
  SkillItem skill, {
  bool closeParentOnSuccess = false,
}) async {
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('删除云端 Skill？'),
      content: Text('“${skill.name}”的云端文件和私人备注将永久删除。'),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: const Text('取消'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, true),
          style: FilledButton.styleFrom(
            backgroundColor: const Color(0xFFAE4B3B),
          ),
          child: const Text('永久删除'),
        ),
      ],
    ),
  );
  if (confirmed == true) {
    final success = await controller.delete(skill);
    if (success && closeParentOnSuccess && context.mounted) {
      Navigator.pop(context);
    }
  }
}

class _DialogAvatar extends StatelessWidget {
  const _DialogAvatar({required this.skill});

  final SkillItem skill;

  @override
  Widget build(BuildContext context) => Container(
    width: 58,
    height: 58,
    decoration: BoxDecoration(
      color: const Color(0xFFE9E1FF),
      borderRadius: BorderRadius.circular(16),
    ),
    child: Center(
      child: Text(
        skill.name.characters.first.toUpperCase(),
        style: const TextStyle(
          color: purple,
          fontSize: 20,
          fontWeight: FontWeight.w900,
        ),
      ),
    ),
  );
}
