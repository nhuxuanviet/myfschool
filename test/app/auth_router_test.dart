import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/auth/application/auth_controller.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_data_providers.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/login_page.dart';
import 'package:myfschoolse1913/src/features/home/presentation/home_page.dart';
import 'package:myfschoolse1913/src/features/home/application/home_providers.dart';
import 'package:myfschoolse1913/src/features/system_health/domain/health_check.dart';
import 'package:myfschoolse1913/src/features/system_health/providers/health_providers.dart';

import '../helpers/fake_auth_repository.dart';
import '../helpers/test_home_dashboard.dart';

void main() {
  testWidgets('blocks an unauthenticated home deep link', (tester) async {
    final container = _container(FakeAuthRepository());
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const FptSchoolsApp(),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(LoginPage), findsOneWidget);

    container.read(appRouterProvider).go(AppRoutes.home);
    await tester.pumpAndSettle();

    expect(find.byType(LoginPage), findsOneWidget);
    expect(find.byType(HomePage), findsNothing);
    await tester.pumpWidget(const SizedBox.shrink());
    container.dispose();
  });

  testWidgets('blocks an unauthenticated student-feature deep link', (
    tester,
  ) async {
    final container = _container(FakeAuthRepository());
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const FptSchoolsApp(),
      ),
    );
    await tester.pumpAndSettle();

    container.read(appRouterProvider).go(AppRoutes.grades);
    await tester.pumpAndSettle();

    expect(find.byType(LoginPage), findsOneWidget);
    expect(find.byType(HomePage), findsNothing);
    await tester.pumpWidget(const SizedBox.shrink());
    container.dispose();
  });

  testWidgets('restores a valid session and redirects to home', (tester) async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession(fullName: 'Phạm Gia Hân');
    final container = _container(repository);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const FptSchoolsApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(HomePage), findsOneWidget);
    expect(find.text('Phạm Gia Hân'), findsOneWidget);

    final sessionId = repository.restoredSession!.sessionId;
    await container
        .read(authControllerProvider.notifier)
        .expireSession(sessionId);
    await tester.pumpAndSettle();
    expect(find.byType(LoginPage), findsOneWidget);

    await tester.pumpWidget(const SizedBox.shrink());
    container.dispose();
  });
}

ProviderContainer _container(FakeAuthRepository repository) {
  return ProviderContainer(
    overrides: [
      authRepositoryProvider.overrideWithValue(repository),
      homeDashboardProvider.overrideWith(
        (ref) async => testHomeDashboard(
          fullName:
              repository.restoredSession?.student.fullName ?? 'Nguyễn Văn A',
        ),
      ),
      healthCheckProvider.overrideWith(
        (ref) async => const HealthCheck(status: HealthStatus.up),
      ),
    ],
  );
}
