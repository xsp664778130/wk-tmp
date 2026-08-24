import 'dart:io';

import 'package:flutter/material.dart';

import 'app_controller.dart';
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
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'SkillPort',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: purple,
          brightness: Brightness.light,
        ),
        scaffoldBackgroundColor: canvas,
        fontFamily: Platform.isWindows
            ? 'Microsoft YaHei UI'
            : '.AppleSystemUIFont',
        inputDecorationTheme: const InputDecorationTheme(
          filled: true,
          fillColor: Color(0xFFF7F5F1),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: line),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: line),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.all(Radius.circular(12)),
            borderSide: BorderSide(color: purple, width: 1.4),
          ),
        ),
      ),
      home: AnimatedBuilder(
        animation: controller,
        builder: (context, _) {
          if (controller.initializing) return const SplashScreen();
          if (!controller.signedIn) return AuthScreen(controller: controller);
          return Workspace(controller: controller);
        },
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
    return Scaffold(
      body: Stack(
        children: <Widget>[
          Row(
            children: <Widget>[
              Expanded(
                flex: 11,
                child: Container(
                  padding: const EdgeInsets.all(64),
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      colors: <Color>[
                        Color(0xFFEDE8FF),
                        Color(0xFFF6F3FF),
                        Color(0xFFF3F8D9),
                      ],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                  ),
                  child: const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
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
                      Spacer(),
                      Text(
                        '你的 AI Skill，\n真正装进这台电脑。',
                        style: TextStyle(
                          fontSize: 44,
                          height: 1.14,
                          fontWeight: FontWeight.w900,
                          letterSpacing: -1.8,
                        ),
                      ),
                      SizedBox(height: 35),
                      Wrap(
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
                      Spacer(),
                      Text(
                        'SkillPort Desktop 1.0.7',
                        style: TextStyle(color: muted),
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
                            style: const TextStyle(color: muted, fontSize: 14),
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
                              backgroundColor: purple,
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
          const Positioned(top: 24, right: 24, child: VersionUpdateButton()),
        ],
      ),
    );
  }
}

class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 42});

  final double size;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      color: ink,
      borderRadius: BorderRadius.circular(size * .3),
    ),
    child: Center(
      child: Text(
        'S',
        style: TextStyle(
          color: Colors.white,
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
      color: Colors.white.withValues(alpha: .72),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Icon(icon, size: 18, color: purple),
        const SizedBox(width: 8),
        Text(text, style: const TextStyle(fontWeight: FontWeight.w700)),
      ],
    ),
  );
}
