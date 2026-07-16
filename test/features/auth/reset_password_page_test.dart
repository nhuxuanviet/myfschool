import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/core/constants/app_strings.dart';
import 'package:myfschoolse1913/src/core/network/api_exception.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/login_page.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/reset_password_page.dart';

import '../../helpers/fake_auth_repository.dart';
import '../../helpers/pump_app.dart';

void main() {
  testWidgets('exposes stable semantics for the phone reset step', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.resetPassword,
    );
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel(AppStrings.resetPassword), findsWidgets);
    expect(find.bySemanticsLabel(AppStrings.phoneNumber), findsOneWidget);
    expect(find.bySemanticsLabel('Gửi mã OTP'), findsOneWidget);
    semantics.dispose();
  });

  testWidgets('rejects a mismatched password confirmation after OTP', (
    tester,
  ) async {
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.resetPassword,
    );
    addTearDown(router.dispose);

    await _advanceToPasswordStep(tester);
    await tester.enterText(
      find.byKey(ResetPasswordPage.newPasswordFieldKey),
      'New-password1',
    );
    await tester.enterText(
      find.byKey(ResetPasswordPage.confirmPasswordFieldKey),
      'different-password',
    );
    await tester.tap(
      find.widgetWithText(ElevatedButton, AppStrings.resetPassword),
    );
    await tester.pump();

    expect(find.text('Mật khẩu xác nhận không khớp.'), findsOneWidget);
    expect(find.byType(ResetPasswordPage), findsOneWidget);
  });

  testWidgets('completes the three-step password reset flow', (tester) async {
    final repository = FakeAuthRepository();
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.resetPassword,
      authRepository: repository,
    );
    addTearDown(router.dispose);

    await _advanceToPasswordStep(tester);
    await tester.enterText(
      find.byKey(ResetPasswordPage.newPasswordFieldKey),
      'New-password1',
    );
    await tester.enterText(
      find.byKey(ResetPasswordPage.confirmPasswordFieldKey),
      'New-password1',
    );
    await tester.tap(
      find.widgetWithText(ElevatedButton, AppStrings.resetPassword),
    );
    await tester.pumpAndSettle();

    expect(find.text('Đặt lại mật khẩu thành công'), findsOneWidget);
    expect(repository.requestResetCalls, 1);
    expect(repository.verifyResetCalls, 1);
    expect(repository.completeResetCalls, 1);
    expect(repository.lastPhoneNumber, '0912345678');
    expect(repository.lastOtp, '123456');
    expect(repository.lastNewPassword, 'New-password1');

    await tester.tap(find.text(AppStrings.backToLogin));
    await tester.pumpAndSettle();
    expect(find.byType(LoginPage), findsOneWidget);
  });

  testWidgets('shows a mapped OTP ProblemDetail error', (tester) async {
    final repository = FakeAuthRepository()
      ..verifyResetError = const ApiException(
        message: 'Invalid OTP',
        statusCode: 400,
        code: 'OTP_INVALID',
      );
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.resetPassword,
      authRepository: repository,
    );
    addTearDown(router.dispose);

    await tester.enterText(
      find.byKey(ResetPasswordPage.phoneFieldKey),
      '0912345678',
    );
    await tester.tap(find.text('Gửi mã OTP'));
    await tester.pumpAndSettle();
    await _enterOtp(tester, '000000');
    await tester.tap(find.text('Xác minh OTP'));
    await tester.pumpAndSettle();

    expect(find.text('Mã OTP không đúng hoặc đã hết hạn.'), findsOneWidget);
    expect(find.byKey(ResetPasswordPage.otpFieldKey), findsOneWidget);
  });
}

Future<void> _advanceToPasswordStep(WidgetTester tester) async {
  await tester.enterText(
    find.byKey(ResetPasswordPage.phoneFieldKey),
    '0912345678',
  );
  await tester.tap(find.text('Gửi mã OTP'));
  await tester.pumpAndSettle();
  expect(find.byKey(ResetPasswordPage.otpFieldKey), findsOneWidget);
  for (var index = 0; index < 6; index++) {
    expect(find.byKey(ValueKey('otp-box-$index')), findsOneWidget);
  }

  await _enterOtp(tester, '123456');
  await tester.tap(find.text('Xác minh OTP'));
  await tester.pumpAndSettle();
  expect(find.byKey(ResetPasswordPage.newPasswordFieldKey), findsOneWidget);
}

Future<void> _enterOtp(WidgetTester tester, String value) async {
  for (var index = 0; index < value.length; index++) {
    final key = index == 0
        ? ResetPasswordPage.otpFieldKey
        : ValueKey('otp-digit-$index');
    await tester.enterText(find.byKey(key), value[index]);
  }
}
