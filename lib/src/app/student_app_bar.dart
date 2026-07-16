import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'app_router.dart';

AppBar studentAppBar({
  required BuildContext context,
  required String title,
  List<Widget> actions = const [],
}) {
  return AppBar(
    leading: IconButton(
      tooltip: 'Quay lại trang chủ',
      onPressed: () => context.go(AppRoutes.home),
      icon: const Icon(Icons.arrow_back),
    ),
    title: ExcludeSemantics(child: Text(title)),
    actions: actions,
  );
}
