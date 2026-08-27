import 'package:desktop_drop/desktop_drop.dart';
import 'package:flutter/material.dart';

import 'app.dart';
import 'app_controller.dart';
import 'app_theme.dart';
import 'dialogs.dart';
import 'feedback_mailbox.dart';
import 'local_installer.dart';
import 'models.dart';
import 'release_notes.dart';

class Workspace extends StatefulWidget {
  const Workspace({super.key, required this.controller});

  final AppController controller;

  @override
  State<Workspace> createState() => _WorkspaceState();
}

class _WorkspaceState extends State<Workspace> {
  int _feedbackId = 0;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final event = widget.controller.feedback;
    if (event != null && event.id != _feedbackId) {
      _feedbackId = event.id;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(event.message),
            backgroundColor: event.error ? scheme.error : scheme.inverseSurface,
            behavior: SnackBarBehavior.floating,
          ),
        );
      });
    }
    return Scaffold(
      body: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 900;
          final showRail = constraints.maxWidth >= 1260;
          return Row(
            children: <Widget>[
              Sidebar(controller: widget.controller, compact: compact),
              Expanded(child: Library(controller: widget.controller)),
              if (showRail) LocalRail(controller: widget.controller),
            ],
          );
        },
      ),
    );
  }
}

class Sidebar extends StatelessWidget {
  const Sidebar({super.key, required this.controller, required this.compact});

  final AppController controller;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final palette = skillPortPalette(context);
    final width = compact ? 82.0 : 238.0;
    Widget navButton({
      required IconData icon,
      required String label,
      required bool selected,
      required VoidCallback onTap,
      int? count,
    }) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 3),
        child: Material(
          color: selected ? scheme.surface : Colors.transparent,
          borderRadius: BorderRadius.circular(11),
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(11),
            child: SizedBox(
              height: 44,
              child: Row(
                mainAxisAlignment: compact
                    ? MainAxisAlignment.center
                    : MainAxisAlignment.start,
                children: <Widget>[
                  if (!compact) const SizedBox(width: 13),
                  Icon(
                    icon,
                    size: 20,
                    color: selected ? scheme.primary : scheme.onSurfaceVariant,
                  ),
                  if (!compact) ...<Widget>[
                    const SizedBox(width: 11),
                    Expanded(
                      child: Text(
                        label,
                        style: TextStyle(
                          fontWeight: selected
                              ? FontWeight.w800
                              : FontWeight.w600,
                          color: selected ? scheme.primary : scheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                    if (count != null)
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 7,
                          vertical: 3,
                        ),
                        decoration: BoxDecoration(
                          color: scheme.primaryContainer,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          '$count',
                          style: TextStyle(
                            fontSize: 11,
                            color: scheme.onPrimaryContainer,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                    const SizedBox(width: 11),
                  ],
                ],
              ),
            ),
          ),
        ),
      );
    }

    return Container(
      width: width,
      color: palette.sidebar,
      padding: EdgeInsets.fromLTRB(
        compact ? 10 : 17,
        24,
        compact ? 10 : 17,
        18,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            mainAxisAlignment: compact
                ? MainAxisAlignment.center
                : MainAxisAlignment.start,
            children: <Widget>[
              const BrandMark(size: 39),
              if (!compact) ...const <Widget>[
                SizedBox(width: 10),
                Text(
                  'skillport.',
                  style: TextStyle(fontSize: 21, fontWeight: FontWeight.w900),
                ),
              ],
            ],
          ),
          const SizedBox(height: 30),
          if (!compact)
            Padding(
              padding: EdgeInsets.only(left: 11, bottom: 6),
              child: Text(
                '探索',
                style: TextStyle(
                  color: scheme.onSurfaceVariant,
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.2,
                ),
              ),
            ),
          navButton(
            icon: Icons.public_rounded,
            label: 'Skill 公有池',
            selected:
                controller.mode == LibraryMode.publicPool &&
                controller.activeCategory == '全部技能',
            count: controller.publicSkills.length,
            onTap: () => controller.setMode(LibraryMode.publicPool),
          ),
          if (!compact)
            ...skillCategories
                .skip(1)
                .map(
                  (category) => navButton(
                    icon: categoryIcon(category),
                    label: category,
                    selected:
                        controller.mode == LibraryMode.publicPool &&
                        controller.activeCategory == category,
                    onTap: () {
                      controller.setMode(LibraryMode.publicPool);
                      controller.setCategory(category);
                    },
                  ),
                ),
          if (!compact)
            Padding(
              padding: EdgeInsets.only(left: 11, top: 24, bottom: 6),
              child: Text(
                '个人空间',
                style: TextStyle(
                  color: scheme.onSurfaceVariant,
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.2,
                ),
              ),
            ),
          navButton(
            icon: Icons.inventory_2_outlined,
            label: '我的 Skill',
            selected: controller.mode == LibraryMode.privateSpace,
            count: controller.privateSkills.length,
            onTap: () => controller.setMode(LibraryMode.privateSpace),
          ),
          const Spacer(),
          if (!compact)
            Container(
              padding: const EdgeInsets.all(13),
              decoration: BoxDecoration(
                color: scheme.surface.withValues(alpha: .72),
                borderRadius: BorderRadius.circular(13),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Icon(
                        Icons.desktop_windows_rounded,
                        size: 18,
                        color: scheme.primary,
                      ),
                      SizedBox(width: 8),
                      Text(
                        'Flutter 本机模式',
                        style: TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${controller.tools.where((tool) => tool.detected).length} 个 AI 工具已识别',
                    style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 11),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 10),
          if (compact)
            IconButton(
              onPressed: controller.busy
                  ? null
                  : () => showFeedbackMailboxDialog(context, controller),
              tooltip: '意见信箱',
              icon: const Icon(Icons.mark_email_unread_outlined),
            )
          else
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: controller.busy
                    ? null
                    : () => showFeedbackMailboxDialog(context, controller),
                style: OutlinedButton.styleFrom(
                  alignment: Alignment.centerLeft,
                  foregroundColor: scheme.primary,
                  side: BorderSide(color: scheme.outlineVariant),
                  backgroundColor: scheme.surface.withValues(alpha: .72),
                ),
                icon: const Icon(Icons.mark_email_unread_outlined, size: 18),
                label: const Text(
                  '意见信箱',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
            ),
          const SizedBox(height: 5),
          if (compact)
            PopupMenuButton<SkillPortThemePreset>(
              tooltip: '切换配色主题',
              initialValue: controller.themePreset,
              onSelected: controller.setThemePreset,
              icon: Icon(Icons.palette_outlined, color: scheme.primary),
              itemBuilder: (context) => SkillPortThemePreset.values
                  .map(
                    (preset) => PopupMenuItem<SkillPortThemePreset>(
                      value: preset,
                      child: Text(preset.label),
                    ),
                  )
                  .toList(),
            )
          else
            SizedBox(
              width: double.infinity,
              child: PopupMenuButton<SkillPortThemePreset>(
                initialValue: controller.themePreset,
                onSelected: controller.setThemePreset,
                position: PopupMenuPosition.over,
                itemBuilder: (context) => SkillPortThemePreset.values
                    .map(
                      (preset) => PopupMenuItem<SkillPortThemePreset>(
                        value: preset,
                        child: ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: CircleAvatar(
                            radius: 12,
                            backgroundColor: preset.previewColor,
                          ),
                          title: Text(preset.label, style: const TextStyle(fontWeight: FontWeight.w800)),
                          subtitle: Text(preset.description),
                          trailing: controller.themePreset == preset
                              ? Icon(Icons.check_rounded, color: scheme.primary)
                              : null,
                        ),
                      ),
                    )
                    .toList(),
                child: Container(
                  height: 39,
                  padding: const EdgeInsets.symmetric(horizontal: 13),
                  decoration: BoxDecoration(
                    color: scheme.surface.withValues(alpha: .72),
                    border: Border.all(color: scheme.outlineVariant),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: <Widget>[
                      Icon(Icons.palette_outlined, size: 18, color: scheme.primary),
                      const SizedBox(width: 9),
                      Expanded(child: Text('配色 · ${controller.themePreset.label}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800))),
                      const Icon(Icons.expand_less_rounded, size: 17),
                    ],
                  ),
                ),
              ),
            ),
          const SizedBox(height: 5),
          IconButton(
            onPressed: controller.busy ? null : controller.logout,
            tooltip: '退出登录',
            icon: const Icon(Icons.logout_rounded),
          ),
        ],
      ),
    );
  }
}

class Library extends StatelessWidget {
  const Library({super.key, required this.controller});

  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final public = controller.mode == LibraryMode.publicPool;
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: <Widget>[
        Container(
          height: 74,
          padding: const EdgeInsets.symmetric(horizontal: 28),
          decoration: BoxDecoration(
            color: scheme.surface.withValues(alpha: .96),
            border: Border(bottom: BorderSide(color: scheme.outlineVariant)),
          ),
          child: Row(
            children: <Widget>[
              Expanded(
                child: TextField(
                  onChanged: controller.setQuery,
                  decoration: const InputDecoration(
                    hintText: '搜索 Skill、分类或用途…',
                    prefixIcon: Icon(Icons.search_rounded),
                    isDense: true,
                  ),
                ),
              ),
              const SizedBox(width: 16),
              const VersionUpdateButton(),
              const SizedBox(width: 8),
              IconButton(
                onPressed: controller.busy ? null : controller.refresh,
                tooltip: '同步云端',
                icon: const Icon(Icons.sync_rounded),
              ),
              const SizedBox(width: 8),
              CircleAvatar(
                backgroundColor: scheme.inverseSurface,
                foregroundColor: scheme.onInverseSurface,
                child: Text(
                  controller.user!.displayName.characters.first.toUpperCase(),
                ),
              ),
              const SizedBox(width: 10),
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    controller.user!.displayName,
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 13,
                    ),
                  ),
                  Text(
                    '独立桌面客户端',
                    style: TextStyle(fontSize: 10, color: scheme.onSurfaceVariant),
                  ),
                ],
              ),
            ],
          ),
        ),
        if (controller.busy)
          LinearProgressIndicator(
            minHeight: 3,
            backgroundColor: Colors.transparent,
            semanticsLabel: controller.busyLabel,
          ),
        Expanded(
          child: CustomScrollView(
            slivers: <Widget>[
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(28, 28, 28, 18),
                sliver: SliverToBoxAdapter(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              public
                                  ? (controller.activeCategory == '全部技能'
                                        ? '社区最新分享'
                                        : controller.activeCategory)
                                  : '我的私人空间',
                              style: const TextStyle(
                                fontSize: 30,
                                fontWeight: FontWeight.w900,
                                letterSpacing: -1.2,
                              ),
                            ),
                            const SizedBox(height: 7),
                            Text(
                              public
                                  ? '按分类发现社区 Skill，拉取后生成你的独立副本。'
                                  : '在客户端内上传、备注、分享并直接安装到本机。',
                              style: TextStyle(color: scheme.onSurfaceVariant),
                            ),
                          ],
                        ),
                      ),
                      if (!public)
                        FilledButton.icon(
                          onPressed: controller.busy
                              ? null
                              : () => showUploadDialog(context, controller),
                          icon: const Icon(Icons.add_rounded),
                          label: const Text('上传 Skill'),
                        ),
                    ],
                  ),
                ),
              ),
              if (!public)
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(28, 0, 28, 20),
                  sliver: SliverToBoxAdapter(
                    child: QuickDrop(controller: controller),
                  ),
                ),
              if (controller.visibleSkills.isEmpty)
                SliverFillRemaining(
                  hasScrollBody: false,
                  child: EmptyState(
                    public: public,
                    onAction: () => controller.setMode(
                      public
                          ? LibraryMode.privateSpace
                          : LibraryMode.publicPool,
                    ),
                  ),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(28, 0, 28, 36),
                  sliver: SliverGrid.builder(
                    itemCount: controller.visibleSkills.length,
                    gridDelegate:
                        const SliverGridDelegateWithMaxCrossAxisExtent(
                          maxCrossAxisExtent: 340,
                          mainAxisExtent: 370,
                          mainAxisSpacing: 14,
                          crossAxisSpacing: 14,
                        ),
                    itemBuilder: (context, index) => SkillCard(
                      controller: controller,
                      skill: controller.visibleSkills[index],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class QuickDrop extends StatefulWidget {
  const QuickDrop({super.key, required this.controller});

  final AppController controller;

  @override
  State<QuickDrop> createState() => _QuickDropState();
}

class _QuickDropState extends State<QuickDrop> {
  bool _dragging = false;

  @override
  Widget build(BuildContext context) => DropTarget(
    onDragEntered: (_) => setState(() => _dragging = true),
    onDragExited: (_) => setState(() => _dragging = false),
    onDragDone: (detail) {
      setState(() => _dragging = false);
      if (detail.files.isNotEmpty) {
        showUploadDialog(
          context,
          widget.controller,
          initialFile: detail.files.first,
        );
      }
    },
    child: AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      height: 64,
      padding: const EdgeInsets.symmetric(horizontal: 18),
      decoration: BoxDecoration(
        color: _dragging
            ? Theme.of(context).colorScheme.primaryContainer
            : Theme.of(context).colorScheme.surface,
        border: Border.all(
          color: _dragging
              ? Theme.of(context).colorScheme.primary
              : Theme.of(context).colorScheme.outlineVariant,
        ),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: <Widget>[
          Icon(
            Icons.file_upload_outlined,
            color: _dragging
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.onSurfaceVariant,
          ),
          const SizedBox(width: 11),
          const Expanded(
            child: Text(
              '把 .zip、.skill 或 SKILL.md 拖到这里快速上传',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
          TextButton(
            onPressed: () => showUploadDialog(context, widget.controller),
            child: const Text('选择文件'),
          ),
        ],
      ),
    ),
  );
}

class SkillCard extends StatelessWidget {
  const SkillCard({super.key, required this.controller, required this.skill});

  final AppController controller;
  final SkillItem skill;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final palette = skillPortPalette(context);
    return Card(
      elevation: 0,
      color: scheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(17),
        side: BorderSide(color: scheme.outlineVariant),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => showSkillDetail(context, controller, skill),
        child: Padding(
          padding: const EdgeInsets.all(17),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  SkillAvatar(controller: controller, skill: skill),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: palette.soft,
                      borderRadius: BorderRadius.circular(7),
                    ),
                    child: Text(
                      skill.category,
                      style: TextStyle(
                        fontSize: 11,
                        color: scheme.onSurfaceVariant,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Text(
                skill.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 7),
              Text(
                skill.description.isEmpty ? '暂无描述' : skill.description,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 13, height: 1.5, color: scheme.onSurfaceVariant),
              ),
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: palette.soft,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: scheme.outlineVariant),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      '详情',
                      style: TextStyle(
                        color: scheme.primary,
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      skill.detail.isEmpty ? skill.description : skill.detail,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: scheme.onSurfaceVariant,
                        fontSize: 12,
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 7),
              Row(
                children: <Widget>[
                  Icon(Icons.format_list_numbered_rounded, size: 15, color: scheme.primary),
                  const SizedBox(width: 5),
                  Text(
                    skill.usageSteps.isEmpty
                        ? '查看完整使用说明'
                        : '${skill.usageSteps.length} 个使用步骤',
                    style: TextStyle(
                      color: scheme.onSurfaceVariant,
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
              if (!skill.isPublic && skill.note.isNotEmpty) ...<Widget>[
                const SizedBox(height: 9),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 9,
                    vertical: 7,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF8DD),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    '备注：${skill.note}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 11,
                      color: Color(0xFF796A2D),
                    ),
                  ),
                ),
              ],
              const Spacer(),
              Row(
                children: <Widget>[
                  Expanded(
                    child: Text(
                      skill.isPublic
                          ? 'by ${skill.author} · ${skill.pullCount} 次拉取'
                          : skill.shared
                          ? '已分享到公有池'
                          : '私人 Skill',
                      style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 11),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              if (skill.isPublic)
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.tonalIcon(
                    onPressed: skill.pulled || controller.busy
                        ? null
                        : () => controller.pull(skill),
                    icon: Icon(
                      skill.pulled ? Icons.check_rounded : Icons.south_rounded,
                      size: 17,
                    ),
                    label: Text(skill.pulled ? '已在我的空间' : '拉取到我的空间'),
                  ),
                )
              else
                Row(
                  children: <Widget>[
                    SizedBox(
                      width: 44,
                      height: 40,
                      child: IconButton(
                        onPressed: controller.busy
                            ? null
                            : () => confirmDeleteSkill(
                                context,
                                controller,
                                skill,
                              ),
                        style: IconButton.styleFrom(
                          foregroundColor: const Color(0xFFA44839),
                          side: const BorderSide(color: Color(0xFFF0D1CB)),
                          backgroundColor: const Color(0xFFFFF9F7),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                        ),
                        tooltip: '删除云端 Skill',
                        icon: const Icon(
                          Icons.delete_outline_rounded,
                          size: 19,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      flex: 2,
                      child: FilledButton.icon(
                        onPressed: controller.busy
                            ? null
                            : () => showInstallDialog(
                                context,
                                controller,
                                skill,
                                LocalAction.install,
                              ),
                        style: FilledButton.styleFrom(backgroundColor: scheme.primary),
                        icon: const Icon(Icons.download_done_rounded, size: 17),
                        label: const Text('安装到本机'),
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class LocalRail extends StatelessWidget {
  const LocalRail({super.key, required this.controller});

  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final palette = skillPortPalette(context);
    return Container(
      width: 286,
      padding: const EdgeInsets.fromLTRB(20, 27, 20, 20),
      decoration: BoxDecoration(
        color: palette.rail,
        border: Border(left: BorderSide(color: scheme.outlineVariant)),
      ),
      child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const Text(
          '本机 AI 工具',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 4),
        Text(
          'Flutter 客户端直接读写本机目录',
          style: TextStyle(fontSize: 11, color: scheme.onSurfaceVariant),
        ),
        const SizedBox(height: 16),
        ...controller.tools.map(
          (tool) => Container(
            margin: const EdgeInsets.only(bottom: 9),
            padding: const EdgeInsets.all(11),
            decoration: BoxDecoration(
              color: scheme.surface,
              border: Border.all(color: scheme.outlineVariant),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: <Widget>[
                CircleAvatar(
                  radius: 18,
                  backgroundColor: tool.detected ? palette.successSurface : palette.soft,
                  foregroundColor: tool.detected ? palette.success : scheme.onSurfaceVariant,
                  child: Text(
                    toolMarks[tool.id] ?? '?',
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        tool.name,
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 12,
                        ),
                      ),
                      Text(
                        tool.detected ? '已识别' : '未检测到',
                        style: TextStyle(
                          fontSize: 10,
                              color: tool.detected ? palette.success : scheme.onSurfaceVariant,
                        ),
                      ),
                      if (tool.detected) ...<Widget>[
                        const SizedBox(height: 6),
                        Tooltip(
                          message: tool.directory,
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 6,
                            ),
                            decoration: BoxDecoration(
                              color: palette.soft,
                              borderRadius: BorderRadius.circular(7),
                            ),
                            child: Row(
                              children: <Widget>[
                                Icon(
                                  Icons.folder_open_rounded,
                                  size: 13,
                                  color: scheme.primary,
                                ),
                                const SizedBox(width: 5),
                                Expanded(
                                  child: Text(
                                    tool.directory,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      fontSize: 9,
                                      height: 1.35,
                                      color: scheme.onSurfaceVariant,
                                      fontFamily: 'monospace',
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        const Divider(height: 28),
        const Text(
          '最近本机操作',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 12),
        if (controller.activities.isEmpty)
          const Text(
            '安装或卸载后会显示在这里。',
            style: TextStyle(fontSize: 11, color: muted),
          )
        else
          ...controller.activities
              .take(5)
              .map(
                (activity) => Padding(
                  padding: const EdgeInsets.only(bottom: 13),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Icon(
                        activity.action == LocalAction.install
                            ? Icons.south_rounded
                            : Icons.delete_outline_rounded,
                        size: 18,
                        color: activity.action == LocalAction.install
                            ? purple
                            : const Color(0xFFB04F40),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          '${activity.action == LocalAction.install ? '安装' : '卸载'} ${activity.skillName}\n${activity.targets.map((id) => toolLabels[id] ?? id).join('、')}',
                          style: const TextStyle(fontSize: 11, height: 1.45),
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

class SkillAvatar extends StatelessWidget {
  const SkillAvatar({
    super.key,
    required this.controller,
    required this.skill,
    this.large = false,
  });

  final AppController controller;
  final SkillItem skill;
  final bool large;

  @override
  Widget build(BuildContext context) {
    final size = large ? 58.0 : 44.0;
    final url = avatarUrl(skill);
    final fallback = Center(
      child: Text(
        skill.name.characters.first.toUpperCase(),
        style: TextStyle(
          color: purple,
          fontSize: large ? 20 : 14,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
    return Container(
      width: size,
      height: size,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: const Color(0xFFE9E1FF),
        borderRadius: BorderRadius.circular(large ? 16 : 13),
      ),
      child: url == null
          ? fallback
          : Image.network(
              url,
              headers: controller.imageHeaders,
              fit: BoxFit.cover,
              errorBuilder: (_, _, _) => fallback,
            ),
    );
  }
}

String? avatarUrl(SkillItem skill) {
  final value = skill.avatarUrl;
  if (value == null || value.isEmpty) return null;
  if (value.startsWith('http://') || value.startsWith('https://')) return value;
  return 'https://www.jmuyuer.com$value';
}

class EmptyState extends StatelessWidget {
  const EmptyState({super.key, required this.public, required this.onAction});

  final bool public;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) => Center(
    child: Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Icon(
          public ? Icons.public_off_outlined : Icons.inventory_2_outlined,
          size: 48,
          color: muted,
        ),
        const SizedBox(height: 13),
        Text(
          public ? '这个分类还没有公开 Skill' : '你的私人空间还没有 Skill',
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 6),
        TextButton(
          onPressed: onAction,
          child: Text(public ? '去我的空间分享' : '浏览 Skill 公有池'),
        ),
      ],
    ),
  );
}

IconData categoryIcon(String category) => switch (category) {
  '测试技能' => Icons.fact_check_outlined,
  '排查技能' => Icons.troubleshoot_rounded,
  '日志技能' => Icons.receipt_long_outlined,
  _ => Icons.code_rounded,
};
