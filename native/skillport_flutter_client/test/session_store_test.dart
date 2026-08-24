import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:skillport_client/src/session_store.dart';

void main() {
  test(
    'macOS file session store persists and clears the token privately',
    () async {
      final root = await Directory.systemTemp.createTemp(
        'skillport-session-store-',
      );
      addTearDown(() => root.delete(recursive: true));
      final store = MacFileSessionStore(supportDirectory: root);

      expect(await store.readToken(), isNull);
      await store.writeToken('session-token-123');
      expect(await store.readToken(), 'session-token-123');

      final token = File('${root.path}/session.token');
      expect(await token.exists(), isTrue);
      final permission = await Process.run('/bin/stat', <String>[
        '-f',
        '%Lp',
        token.path,
      ]);
      expect(permission.exitCode, 0);
      expect((permission.stdout as String).trim(), '600');

      await store.clearToken();
      expect(await store.readToken(), isNull);
    },
    skip: !Platform.isMacOS,
  );
}
