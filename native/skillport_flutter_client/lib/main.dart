import 'package:flutter/material.dart';

import 'src/api_client.dart';
import 'src/app.dart';
import 'src/app_controller.dart';
import 'src/local_installer.dart';
import 'src/session_store.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final controller = AppController(
    api: SkillPortApi(),
    sessionStore: const SecureSessionStore(),
    installer: LocalInstaller(),
  );
  runApp(SkillPortApp(controller: controller));
  await controller.initialize();
}
