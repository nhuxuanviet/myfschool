import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'src/app/app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ProviderScope(child: FptSchoolsApp()));
}

/// Kept as a compatibility alias for existing tests and integrations.
class MyApp extends FptSchoolsApp {
  const MyApp({super.key, super.router});
}
