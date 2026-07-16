import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/auth/presentation/login_page.dart';
import 'package:myfschoolse1913/src/features/home/presentation/home_page.dart';
import 'package:myfschoolse1913/src/features/profile/presentation/profile_page.dart';
import 'package:myfschoolse1913/src/features/grades/presentation/grades_page.dart';
import 'package:myfschoolse1913/src/features/events/presentation/events_page.dart';

import '../../helpers/fake_auth_repository.dart';
import '../../helpers/pump_app.dart';
import '../../helpers/test_home_dashboard.dart';

void main() {
  testWidgets('opens the student profile and logs out', (tester) async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession(fullName: 'Trần Minh Anh');
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.home,
      authRepository: repository,
      homeDashboard: testHomeDashboard(fullName: 'Trần Minh Anh'),
    );
    addTearDown(router.dispose);

    expect(find.byType(HomePage), findsOneWidget);
    expect(find.text('Trần Minh Anh'), findsOneWidget);
    expect(find.bySemanticsLabel('Mở trang cá nhân'), findsOneWidget);

    await tester.tap(find.bySemanticsLabel('Mở trang cá nhân'));
    await tester.pumpAndSettle();
    expect(find.byType(ProfilePage), findsOneWidget);

    await tester.tap(find.text('Đăng xuất'));
    await tester.pumpAndSettle();

    expect(repository.logoutCalls, 1);
    expect(find.byType(LoginPage), findsOneWidget);
  });

  testWidgets('opens a linked student feature from the homepage', (
    tester,
  ) async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession();
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.home,
      authRepository: repository,
    );
    addTearDown(router.dispose);

    final gradesAction = find.text('Kết quả').first;
    await tester.ensureVisible(gradesAction);
    await tester.tap(gradesAction);
    await tester.pumpAndSettle();

    expect(find.byType(GradesPage), findsOneWidget);
  });

  testWidgets('opens events from the homepage and selects the events tab', (
    tester,
  ) async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession();
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.home,
      authRepository: repository,
    );
    addTearDown(router.dispose);

    final eventsAction = find.text('Hoạt động');
    await tester.tap(eventsAction);
    await tester.pumpAndSettle();

    expect(find.byType(EventsPage), findsOneWidget);
    expect(
      tester.widget<NavigationBar>(find.byType(NavigationBar)).selectedIndex,
      3,
    );
  });

  testWidgets('exposes dashboard data through stable semantic boundaries', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession(fullName: 'Trần Minh Anh');
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.home,
      authRepository: repository,
      homeDashboard: testHomeDashboard(fullName: 'Trần Minh Anh'),
    );
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel('Trang chủ'), findsOneWidget);
    expect(find.text('Lịch học'), findsWidgets);
    semantics.dispose();
  });

  testWidgets('renders a compact curved home header', (tester) async {
    final repository = FakeAuthRepository()
      ..restoredSession = testAuthSession();
    final router = await tester.pumpFptSchoolsApp(
      initialLocation: AppRoutes.home,
      authRepository: repository,
    );
    addTearDown(router.dispose);

    final header = find.byKey(const Key('home-header'));
    expect(header, findsOneWidget);
    expect(tester.getSize(header).height, 176);
    expect(tester.widget<ClipPath>(header).clipper, isNotNull);
  });
}
