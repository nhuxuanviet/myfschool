import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/constants/app_colors.dart';
import 'package:myfschoolse1913/src/core/constants/app_strings.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/login_page.dart';

import '../helpers/pump_app.dart';

void main() {
  testWidgets('starts on the branded login screen', (tester) async {
    final semantics = tester.ensureSemantics();
    final router = await tester.pumpFptSchoolsApp();
    addTearDown(router.dispose);

    expect(find.byType(LoginPage), findsOneWidget);
    expect(find.bySemanticsLabel(AppStrings.loginScreen), findsOneWidget);
    expect(find.bySemanticsLabel(AppStrings.appName), findsOneWidget);

    final context = tester.element(find.byType(LoginPage));
    expect(Theme.of(context).colorScheme.primary, AppColors.primary);
    semantics.dispose();
  });
}
