import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'app.dart';
import 'app_controller.dart';

const feedbackKinds = <String>['功能建议', '问题反馈', '体验优化', '其他'];

Future<void> showFeedbackMailboxDialog(
  BuildContext context,
  AppController controller,
) => showDialog<void>(
  context: context,
  barrierDismissible: false,
  builder: (_) => FeedbackMailboxDialog(controller: controller),
);

enum _FeedbackStage { writing, sending, sent }

class FeedbackMailboxDialog extends StatefulWidget {
  const FeedbackMailboxDialog({super.key, required this.controller});

  final AppController controller;

  @override
  State<FeedbackMailboxDialog> createState() =>
      _FeedbackMailboxDialogState();
}

class _FeedbackMailboxDialogState extends State<FeedbackMailboxDialog>
    with SingleTickerProviderStateMixin {
  final TextEditingController _content = TextEditingController();
  late final AnimationController _faxAnimation;
  String _kind = feedbackKinds.first;
  String? _error;
  _FeedbackStage _stage = _FeedbackStage.writing;

  @override
  void initState() {
    super.initState();
    _faxAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    );
    _content.addListener(_refreshCounter);
  }

  void _refreshCounter() => setState(() => _error = null);

  @override
  void dispose() {
    _content
      ..removeListener(_refreshCounter)
      ..dispose();
    _faxAnimation.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final content = _content.text.trim();
    if (content.length < 5) {
      setState(() => _error = '请至少填写 5 个字，让我们更准确地理解你的想法。');
      return;
    }
    setState(() {
      _stage = _FeedbackStage.sending;
      _error = null;
    });
    final results = await Future.wait<Object>(<Future<Object>>[
      widget.controller
          .submitFeedback(_kind, content)
          .then<Object>((value) => value),
      _faxAnimation.forward(from: 0).then<Object>((_) => true),
    ]);
    if (!mounted) return;
    if (results.first == true) {
      setState(() => _stage = _FeedbackStage.sent);
    } else {
      _faxAnimation.reset();
      setState(() {
        _stage = _FeedbackStage.writing;
        _error = '意见暂时没有送达，请检查网络后重试。';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      constraints: const BoxConstraints(maxWidth: 520),
      titlePadding: const EdgeInsets.fromLTRB(28, 25, 18, 0),
      contentPadding: const EdgeInsets.fromLTRB(28, 18, 28, 26),
      title: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            width: 43,
            height: 43,
            decoration: BoxDecoration(
              color: const Color(0xFFECE7FF),
              borderRadius: BorderRadius.circular(13),
            ),
            child: const Icon(Icons.mark_email_unread_outlined, color: purple),
          ),
          const SizedBox(width: 13),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'FEEDBACK MAILBOX',
                  style: TextStyle(
                    fontSize: 10,
                    color: purple,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 1.35,
                  ),
                ),
                SizedBox(height: 3),
                Text('意见信箱', style: TextStyle(fontWeight: FontWeight.w900)),
              ],
            ),
          ),
          if (_stage != _FeedbackStage.sending)
            IconButton(
              onPressed: () => Navigator.pop(context),
              tooltip: '关闭',
              icon: const Icon(Icons.close_rounded),
            ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 260),
          child: _stage == _FeedbackStage.writing
              ? _buildForm()
              : _FaxTransmission(
                  key: ValueKey<_FeedbackStage>(_stage),
                  animation: _faxAnimation,
                  sent: _stage == _FeedbackStage.sent,
                  onDone: () => Navigator.pop(context),
                ),
        ),
      ),
    );
  }

  Widget _buildForm() {
    return Column(
      key: const ValueKey<String>('feedback-form'),
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: <Widget>[
        const Text(
          '每一封意见都会进入 SkillPort 的产品信箱，帮助我们决定下一次改进。',
          style: TextStyle(color: muted, height: 1.5),
        ),
        const SizedBox(height: 18),
        DropdownButtonFormField<String>(
          initialValue: _kind,
          decoration: const InputDecoration(labelText: '意见类型'),
          items: feedbackKinds
              .map(
                (kind) => DropdownMenuItem<String>(
                  value: kind,
                  child: Text(kind),
                ),
              )
              .toList(),
          onChanged: (value) => setState(() => _kind = value ?? _kind),
        ),
        const SizedBox(height: 14),
        TextField(
          controller: _content,
          autofocus: true,
          minLines: 5,
          maxLines: 7,
          maxLength: 2000,
          decoration: const InputDecoration(
            labelText: '写下你的想法',
            alignLabelWithHint: true,
            hintText: '例如：希望可以批量选择多个 Skill，一次安装到 Codex…',
          ),
        ),
        if (_error != null) ...<Widget>[
          const SizedBox(height: 8),
          Text(
            _error!,
            style: const TextStyle(color: Color(0xFFAA493B), fontSize: 12),
          ),
        ],
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: _content.text.trim().length < 5 ? null : _submit,
          style: FilledButton.styleFrom(
            minimumSize: const Size.fromHeight(47),
            backgroundColor: purple,
          ),
          icon: const Icon(Icons.send_rounded),
          label: const Text('发送意见'),
        ),
      ],
    );
  }
}

class _FaxTransmission extends StatelessWidget {
  const _FaxTransmission({
    super.key,
    required this.animation,
    required this.sent,
    required this.onDone,
  });

  final Animation<double> animation;
  final bool sent;
  final VoidCallback onDone;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        SizedBox(
          height: 232,
          child: AnimatedBuilder(
            animation: animation,
            builder: (context, _) {
              final progress = sent ? 1.0 : animation.value;
              final feed = Curves.easeInCubic.transform(
                (progress / .78).clamp(0.0, 1.0),
              );
              final paperOpacity = progress < .67
                  ? 1.0
                  : (1 - ((progress - .67) / .24)).clamp(0.0, 1.0);
              return Stack(
                alignment: Alignment.topCenter,
                children: <Widget>[
                  Positioned(
                    top: 4 + 92 * feed,
                    child: Opacity(
                      opacity: paperOpacity,
                      child: Transform.scale(
                        scaleY: 1 - .78 * feed,
                        alignment: Alignment.bottomCenter,
                        child: const _FaxPaper(),
                      ),
                    ),
                  ),
                  Positioned(
                    left: 0,
                    right: 0,
                    bottom: 18,
                    child: _FaxMachine(progress: progress, sent: sent),
                  ),
                ],
              );
            },
          ),
        ),
        Text(
          sent ? '意见已送达' : '正在传真你的意见…',
          style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 5),
        Text(
          sent ? '感谢你认真写下这封信，我们会仔细阅读。' : '纸张扫描中，请稍候。',
          style: const TextStyle(color: muted),
        ),
        if (sent) ...<Widget>[
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onDone,
            style: FilledButton.styleFrom(
              minimumSize: const Size(180, 43),
              backgroundColor: purple,
            ),
            child: const Text('完成'),
          ),
        ],
      ],
    );
  }
}

class _FaxPaper extends StatelessWidget {
  const _FaxPaper();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 126,
      height: 132,
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFEF9),
        border: Border.all(color: const Color(0xFFD8D3CA)),
        borderRadius: BorderRadius.circular(4),
        boxShadow: const <BoxShadow>[
          BoxShadow(color: Color(0x1F352F3C), blurRadius: 18, offset: Offset(0, 8)),
        ],
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'SKILLPORT',
            style: TextStyle(
              color: purple,
              fontSize: 9,
              fontWeight: FontWeight.w900,
              letterSpacing: 1.2,
            ),
          ),
          SizedBox(height: 13),
          _PaperLine(),
          SizedBox(height: 8),
          _PaperLine(width: 70),
          SizedBox(height: 8),
          _PaperLine(width: 51),
          SizedBox(height: 11),
          Text('你的意见', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

class _PaperLine extends StatelessWidget {
  const _PaperLine({this.width = double.infinity});

  final double width;

  @override
  Widget build(BuildContext context) => Container(
    width: width,
    height: 3,
    decoration: BoxDecoration(
      color: const Color(0xFFDED9E4),
      borderRadius: BorderRadius.circular(3),
    ),
  );
}

class _FaxMachine extends StatelessWidget {
  const _FaxMachine({required this.progress, required this.sent});

  final double progress;
  final bool sent;

  @override
  Widget build(BuildContext context) {
    final pulse = .5 + .5 * math.sin(progress * math.pi * 8);
    return Center(
      child: Container(
        width: 244,
        height: 116,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: <Color>[Color(0xFFF5F1FF), Color(0xFFE6DFF8)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          border: Border.all(color: const Color(0xFFD7D0E6)),
          borderRadius: const BorderRadius.vertical(
            top: Radius.circular(22),
            bottom: Radius.circular(15),
          ),
          boxShadow: const <BoxShadow>[
            BoxShadow(color: Color(0x2B4A3885), blurRadius: 28, offset: Offset(0, 14)),
          ],
        ),
        child: Column(
          children: <Widget>[
            Row(
              children: <Widget>[
                Container(
                  width: 9,
                  height: 9,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: sent
                        ? const Color(0xFF6AAA48)
                        : Color.lerp(
                            const Color(0xFFF0C84F),
                            const Color(0xFFFF6B51),
                            pulse,
                          ),
                    boxShadow: <BoxShadow>[
                      BoxShadow(
                        color: sent
                            ? const Color(0x446AAA48)
                            : const Color(0x55FF6B51),
                        blurRadius: 9,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  sent ? 'DELIVERED' : 'TRANSMITTING',
                  style: const TextStyle(
                    color: Color(0xFF665F72),
                    fontSize: 9,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 1,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 13),
            ClipRRect(
              borderRadius: BorderRadius.circular(9),
              child: Container(
                height: 18,
                color: const Color(0xFF302D35),
                child: Align(
                  alignment: Alignment(-1 + 2 * progress, 0),
                  child: Container(
                    width: 64,
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        colors: <Color>[
                          Colors.transparent,
                          Color(0xFFAA91FF),
                          Colors.transparent,
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 13),
            const Row(
              children: <Widget>[
                _FaxControl(width: 61, active: true),
                SizedBox(width: 8),
                _FaxControl(),
                SizedBox(width: 8),
                _FaxControl(),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _FaxControl extends StatelessWidget {
  const _FaxControl({this.width = 27, this.active = false});

  final double width;
  final bool active;

  @override
  Widget build(BuildContext context) => Container(
    width: width,
    height: 10,
    decoration: BoxDecoration(
      color: active ? const Color(0xFF7862CF) : const Color(0xFFC8BFDD),
      borderRadius: BorderRadius.circular(5),
    ),
  );
}
