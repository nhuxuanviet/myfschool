import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_exception.dart';
import 'package:myfschoolse1913/src/features/auth/application/auth_controller.dart';
import 'package:myfschoolse1913/src/features/auth/application/password_reset_controller.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_data_providers.dart';

import '../../helpers/fake_auth_repository.dart';

void main() {
  test('restores a persisted authenticated session', () async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession(fullName: 'Lê Hà My');
    final container = ProviderContainer(
      overrides: [authRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);

    final state = await container.read(authControllerProvider.future);

    expect(state.isAuthenticated, isTrue);
    expect(state.session?.student.fullName, 'Lê Hà My');
  });

  test('maps login ProblemDetail and does not create a session', () async {
    final repository = FakeAuthRepository()
      ..loginError = const ApiException(
        message: 'Unauthorized',
        statusCode: 401,
        code: 'INVALID_CREDENTIALS',
      );
    final container = ProviderContainer(
      overrides: [authRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    await container.read(authControllerProvider.future);

    final success = await container
        .read(authControllerProvider.notifier)
        .login(phoneNumber: '0912345678', password: 'wrong-password');
    final state = container.read(authControllerProvider).requireValue;

    expect(success, isFalse);
    expect(state.isAuthenticated, isFalse);
    expect(state.errorMessage, 'Số điện thoại hoặc mật khẩu không đúng.');
  });

  test('logs out locally even when the remote request fails', () async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession()
      ..logoutError = StateError('offline');
    final container = ProviderContainer(
      overrides: [authRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    await container.read(authControllerProvider.future);

    await container.read(authControllerProvider.notifier).logout();

    expect(
      container.read(authControllerProvider).requireValue.isAuthenticated,
      isFalse,
    );
    expect(repository.logoutCalls, 1);
  });

  test('does not adopt a refreshed session from an older login', () async {
    final currentSession = testAuthSession(
      accessToken: 'current-access',
      refreshToken: 'current-refresh',
    );
    final repository = FakeAuthRepository()..restoredSession = currentSession;
    final container = ProviderContainer(
      overrides: [authRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    await container.read(authControllerProvider.future);

    container
        .read(authControllerProvider.notifier)
        .adoptRefreshedSession(
          testAuthSession(
            accessToken: 'stale-refreshed-access',
            refreshToken: 'stale-refreshed-token',
            sessionId: 'older-login-session',
          ),
        );

    expect(
      container.read(authControllerProvider).requireValue.session,
      same(currentSession),
    );
  });

  test(
    'password reset controller advances through all three API steps',
    () async {
      final repository = FakeAuthRepository();
      final container = ProviderContainer(
        overrides: [authRepositoryProvider.overrideWithValue(repository)],
      );
      addTearDown(container.dispose);
      final subscription = container.listen(
        passwordResetControllerProvider,
        (_, _) {},
        fireImmediately: true,
      );
      addTearDown(subscription.close);
      final controller = container.read(
        passwordResetControllerProvider.notifier,
      );

      expect(await controller.requestOtp('0912345678'), isTrue);
      expect(
        container.read(passwordResetControllerProvider).step,
        PasswordResetStep.otp,
      );
      expect(await controller.verifyOtp('123456'), isTrue);
      expect(
        container.read(passwordResetControllerProvider).step,
        PasswordResetStep.newPassword,
      );
      expect(await controller.complete('New-password1'), isTrue);
      expect(
        container.read(passwordResetControllerProvider).step,
        PasswordResetStep.success,
      );
    },
  );
}
