import 'package:flutter/material.dart';

import 'app_controller.dart';
import 'app_theme.dart';
import 'release_notes.dart';
import 'workspace.dart';

const purple = Color(0xFF7457E8);
const ink = Color(0xFF29272E);
const canvas = Color(0xFFFAF9F6);
const line = Color(0xFFE6E2DB);
const muted = Color(0xFF77737C);

class SkillPortApp extends StatelessWidget {
  const SkillPortApp({super.key, required this.controller});

  final AppController controller;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, _) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'SkillPort',
        theme: buildSkillPortTheme(controller.themePreset),
        home: Builder(
          builder: (context) {
          if (controller.initializing) return const SplashScreen();
          if (!controller.signedIn) return AuthScreen(controller: controller);
          return Workspace(controller: controller);
          },
        ),
      ),
    );
  }
}

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) => const Scaffold(
    body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          BrandMark(size: 62),
          SizedBox(height: 20),
          Text(
            'SkillPort',
            style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800),
          ),
          SizedBox(height: 20),
          SizedBox(width: 180, child: LinearProgressIndicator(minHeight: 3)),
        ],
      ),
    ),
  );
}

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key, required this.controller});

  final AppController controller;

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _displayName = TextEditingController();
  bool _register = false;
  bool _obscure = true;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    _displayName.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_email.text.trim().isEmpty ||
        _password.text.length < 8 ||
        (_register && _displayName.text.trim().isEmpty)) {
      setState(() => _error = '请完整填写信息，密码至少 8 位。');
      return;
    }
    final success = _register
        ? await widget.controller.register(
            email: _email.text,
            displayName: _displayName.text,
            password: _password.text,
          )
        : await widget.controller.login(_email.text, _password.text);
    if (!success && mounted) {
      setState(() => _error = widget.controller.feedback?.message ?? '登录失败');
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final palette = skillPortPalette(context);
    return Scaffold(
      body: Stack(
        children: <Widget>[
          ThemeGlowBackdrop(preset: widget.controller.themePreset),
          Row(
            children: <Widget>[
              Expanded(
                flex: 11,
                child: Container(
                  padding: const EdgeInsets.all(64),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: <Color>[
                        palette.sidebar,
                        scheme.surface,
                        Color.lerp(scheme.surface, scheme.primary, .08)!,
                      ],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      const Row(
                        children: <Widget>[
                          BrandMark(),
                          SizedBox(width: 12),
                          Text(
                            'skillport.',
                            style: TextStyle(
                              fontSize: 25,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ],
                      ),
                      const Spacer(),
                      const Text(
                        '你的 AI Skill，\n真正装进这台电脑。',
                        style: TextStyle(
                          fontSize: 44,
                          height: 1.14,
                          fontWeight: FontWeight.w900,
                          letterSpacing: -1.8,
                        ),
                      ),
                      const SizedBox(height: 35),
                      const Wrap(
                        spacing: 10,
                        runSpacing: 10,
                        children: <Widget>[
                          FeaturePill(
                            icon: Icons.verified_user_outlined,
                            text: '账户数据隔离',
                          ),
                          FeaturePill(
                            icon: Icons.download_done_rounded,
                            text: 'SHA-256 校验',
                          ),
                          FeaturePill(
                            icon: Icons.computer_rounded,
                            text: 'macOS · Windows',
                          ),
                        ],
                      ),
                      const Spacer(),
                      Text(
                        'SkillPort Desktop $currentReleaseVersion',
                        style: TextStyle(color: scheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
              ),
              Expanded(
                flex: 9,
                child: Center(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(42),
                    child: SizedBox(
                      width: 420,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: <Widget>[
                          Text(
                            _register ? '创建私人账户' : '登录 SkillPort',
                            style: const TextStyle(
                              fontSize: 30,
                              fontWeight: FontWeight.w800,
                              letterSpacing: -1,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            _register
                                ? '你的 Skill、备注和会话均按账户隔离。'
                                : '登录后管理云端 Skill，并直接安装到本机。',
                            style: TextStyle(color: scheme.onSurfaceVariant, fontSize: 14),
                          ),
                          const SizedBox(height: 30),
                          if (_register) ...<Widget>[
                            TextField(
                              controller: _displayName,
                              decoration: const InputDecoration(
                                labelText: '显示名称',
                                prefixIcon: Icon(Icons.badge_outlined),
                              ),
                            ),
                            const SizedBox(height: 14),
                          ],
                          TextField(
                            controller: _email,
                            keyboardType: TextInputType.emailAddress,
                            decoration: const InputDecoration(
                              labelText: '邮箱',
                              prefixIcon: Icon(Icons.mail_outline_rounded),
                            ),
                          ),
                          const SizedBox(height: 14),
                          TextField(
                            controller: _password,
                            obscureText: _obscure,
                            onSubmitted: (_) => _submit(),
                            decoration: InputDecoration(
                              labelText: '密码',
                              prefixIcon: const Icon(
                                Icons.lock_outline_rounded,
                              ),
                              suffixIcon: IconButton(
                                onPressed: () =>
                                    setState(() => _obscure = !_obscure),
                                icon: Icon(
                                  _obscure
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                              ),
                            ),
                          ),
                          if (_error != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 14),
                              child: Text(
                                _error!,
                                style: const TextStyle(
                                  color: Color(0xFFB34232),
                                ),
                              ),
                            ),
                          const SizedBox(height: 20),
                          FilledButton.icon(
                            onPressed: widget.controller.busy ? null : _submit,
                            style: FilledButton.styleFrom(
                              minimumSize: const Size.fromHeight(49),
                              backgroundColor: scheme.primary,
                            ),
                            icon: widget.controller.busy
                                ? const SizedBox.square(
                                    dimension: 17,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Colors.white,
                                    ),
                                  )
                                : const Icon(Icons.arrow_forward_rounded),
                            label: Text(
                              widget.controller.busy
                                  ? widget.controller.busyLabel
                                  : _register
                                  ? '创建并登录'
                                  : '登录客户端',
                            ),
                          ),
                          if (!_register)
                            TextButton(
                              onPressed: widget.controller.busy
                                  ? null
                                  : () => showDialog<void>(
                                      context: context,
                                      barrierDismissible: false,
                                      builder: (_) => ForgotPasswordDialog(
                                        controller: widget.controller,
                                        initialEmail: _email.text,
                                      ),
                                    ),
                              child: const Text('忘记密码？使用邮箱验证码重置'),
                            ),
                          const SizedBox(height: 12),
                          TextButton(
                            onPressed: widget.controller.busy
                                ? null
                                : () => setState(() {
                                    _register = !_register;
                                    _error = null;
                                  }),
                            child: Text(_register ? '已有账户？返回登录' : '没有账户？注册新账户'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
          Positioned(
            top: 18,
            right: 180,
            child: PopupMenuButton<SkillPortThemePreset>(
              tooltip: '切换配色主题',
              initialValue: widget.controller.themePreset,
              onSelected: widget.controller.setThemePreset,
              itemBuilder: (context) => SkillPortThemePreset.values
                  .map(
                    (preset) => PopupMenuItem<SkillPortThemePreset>(
                      value: preset,
                      child: Row(
                        children: <Widget>[
                          ThemePresetSwatch(preset: preset, size: 18),
                          const SizedBox(width: 10),
                          Text(preset.label),
                          if (widget.controller.themePreset == preset) ...<Widget>[
                            const Spacer(),
                            Icon(Icons.check_rounded, color: scheme.primary, size: 18),
                          ],
                        ],
                      ),
                    ),
                  )
                  .toList(),
              icon: Icon(Icons.palette_outlined, color: scheme.primary),
            ),
          ),
          const Positioned(top: 24, right: 24, child: VersionUpdateButton()),
        ],
      ),
    );
  }
}

class ForgotPasswordDialog extends StatefulWidget {
  const ForgotPasswordDialog({
    super.key,
    required this.controller,
    required this.initialEmail,
  });

  final AppController controller;
  final String initialEmail;

  @override
  State<ForgotPasswordDialog> createState() => _ForgotPasswordDialogState();
}

class _ForgotPasswordDialogState extends State<ForgotPasswordDialog> {
  late final TextEditingController _email = TextEditingController(text: widget.initialEmail);
  final _code = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  bool _sent = false;
  String? _error;

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    if (!_email.text.contains('@')) {
      setState(() => _error = '请输入正确的注册邮箱。');
      return;
    }
    final success = await widget.controller.requestPasswordResetCode(_email.text);
    if (!mounted) return;
    setState(() {
      _sent = success;
      _error = success ? null : widget.controller.feedback?.message;
    });
  }

  Future<void> _reset() async {
    if (_code.text.length != 6 || _password.text.length < 8 || _password.text != _confirmPassword.text) {
      setState(() => _error = '请填写 6 位验证码，且两次新密码保持一致。');
      return;
    }
    final success = await widget.controller.resetPassword(
      email: _email.text,
      code: _code.text,
      newPassword: _password.text,
    );
    if (!mounted) return;
    if (success) {
      Navigator.pop(context);
    } else {
      setState(() => _error = widget.controller.feedback?.message);
    }
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('重置登录密码'),
    content: SizedBox(
      width: 430,
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            TextField(
              controller: _email,
              enabled: !_sent,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: '注册邮箱', prefixIcon: Icon(Icons.mail_outline_rounded)),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: widget.controller.busy ? null : _sendCode,
                icon: const Icon(Icons.mark_email_read_outlined),
                label: Text(_sent ? '重新发送验证码' : '发送邮箱验证码'),
              ),
            ),
            if (_sent) ...<Widget>[
              const SizedBox(height: 12),
              TextField(
                controller: _code,
                keyboardType: TextInputType.number,
                maxLength: 6,
                decoration: const InputDecoration(labelText: '6 位验证码', prefixIcon: Icon(Icons.verified_outlined)),
              ),
              const SizedBox(height: 4),
              TextField(
                controller: _password,
                obscureText: true,
                decoration: const InputDecoration(labelText: '新密码', prefixIcon: Icon(Icons.lock_reset_rounded)),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _confirmPassword,
                obscureText: true,
                decoration: const InputDecoration(labelText: '确认新密码', prefixIcon: Icon(Icons.lock_outline_rounded)),
              ),
            ],
            if (_error != null) ...<Widget>[
              const SizedBox(height: 12),
              Align(alignment: Alignment.centerLeft, child: Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error))),
            ],
          ],
        ),
      ),
    ),
    actions: <Widget>[
      TextButton(onPressed: widget.controller.busy ? null : () => Navigator.pop(context), child: const Text('关闭')),
      if (_sent) FilledButton(onPressed: widget.controller.busy ? null : _reset, child: const Text('确认重置')),
    ],
  );
}

class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 42});

  final double size;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.inverseSurface,
      borderRadius: BorderRadius.circular(size * .3),
    ),
    child: Center(
      child: Text(
        'S',
        style: TextStyle(
          color: Theme.of(context).colorScheme.onInverseSurface,
          fontSize: size * .36,
          fontWeight: FontWeight.w900,
        ),
      ),
    ),
  );
}

class FeaturePill extends StatelessWidget {
  const FeaturePill({super.key, required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 10),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.surface.withValues(alpha: .78),
      border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Icon(icon, size: 18, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 8),
        Text(text, style: const TextStyle(fontWeight: FontWeight.w700)),
      ],
    ),
  );
}
