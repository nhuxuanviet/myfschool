import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/constants/app_strings.dart';
import 'package:myfschoolse1913/src/core/network/api_exception.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/login_page.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/reset_password_page.dart';
import 'package:myfschoolse1913/src/features/home/presentation/home_page.dart';

import '../../helpers/pump_app.dart';
import '../../helpers/fake_auth_repository.dart';

void main() {
  testWidgets('exposes stable semantics for login controls', (tester) async {
    final semantics = tester.ensureSemantics();
    final router = await tester.pumpFptSchoolsApp();
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel(AppStrings.phoneNumber), findsOneWidget);
    expect(find.bySemanticsLabel(AppStrings.password), findsOneWidget);
    expect(find.bySemanticsLabel(AppStrings.login), findsOneWidget);
    expect(find.bySemanticsLabel(AppStrings.forgotPassword), findsOneWidget);
    expect(find.bySemanticsLabel(AppStrings.systemOnline), findsOneWidget);
    semantics.dispose();
  });

  testWidgets('validates required credentials', (tester) async {
    final router = await tester.pumpFptSchoolsApp();
    addTearDown(router.dispose);

    await tester.tap(find.text(AppStrings.login));
    await tester.pump();

    expect(find.text('Vui lòng nhập số điện thoại.'), findsOneWidget);
    expect(find.text('Vui lòng nhập mật khẩu.'), findsOneWidget);
    expect(find.byType(HomePage), findsNothing);
  });

  testWidgets('toggles password visibility', (tester) async {
    final router = await tester.pumpFptSchoolsApp();
    addTearDown(router.dispose);

    expect(find.byTooltip('Hiện mật khẩu'), findsOneWidget);
    await tester.tap(find.byTooltip('Hiện mật khẩu'));
    await tester.pump();
    expect(find.byTooltip('Ẩn mật khẩu'), findsOneWidget);
  });

  testWidgets('navigates from login to reset password and back', (
    tester,
  ) async {
    final router = await tester.pumpFptSchoolsApp();
    addTearDown(router.dispose);

    await tester.tap(find.text(AppStrings.forgotPassword));
    await tester.pumpAndSettle();
    expect(find.byType(ResetPasswordPage), findsOneWidget);

    await tester.tap(find.text(AppStrings.backToLogin));
    await tester.pumpAndSettle();
    expect(find.byType(LoginPage), findsOneWidget);
  });

  testWidgets('authenticates through the repository before opening home', (
    tester,
  ) async {
    final repository = FakeAuthRepository();
    final router = await tester.pumpFptSchoolsApp(authRepository: repository);
    addTearDown(router.dispose);

    await tester.enterText(find.byKey(LoginPage.phoneFieldKey), '0912345678');
    await tester.enterText(
      find.byKey(LoginPage.passwordFieldKey),
      'student-password',
    );
    await tester.tap(find.text(AppStrings.login));
    await tester.pumpAndSettle();

    expect(find.byType(HomePage), findsOneWidget);
    expect(find.bySemanticsLabel('Trang chủ'), findsOneWidget);
    expect(repository.loginCalls, 1);
    expect(repository.lastPhoneNumber, '0912345678');
    expect(repository.lastPassword, 'student-password');
  });

  testWidgets('shows the mapped ProblemDetail login error', (tester) async {
    final repository = FakeAuthRepository()
      ..loginError = const ApiException(
        message: 'Unauthorized',
        statusCode: 401,
        code: 'INVALID_CREDENTIALS',
      );
    final router = await tester.pumpFptSchoolsApp(authRepository: repository);
    addTearDown(router.dispose);

    await tester.enterText(find.byKey(LoginPage.phoneFieldKey), '0912345678');
    await tester.enterText(
      find.byKey(LoginPage.passwordFieldKey),
      'wrong-password',
    );
    await tester.tap(find.text(AppStrings.login));
    await tester.pumpAndSettle();

    expect(
      find.text('Số điện thoại hoặc mật khẩu không đúng.'),
      findsOneWidget,
    );
    expect(find.byType(LoginPage), findsOneWidget);
    expect(find.byType(HomePage), findsNothing);
  });
}
