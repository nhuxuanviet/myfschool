import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_data_providers.dart';
import 'package:myfschoolse1913/src/features/auth/domain/auth_repository.dart';
import 'package:myfschoolse1913/src/features/events/application/events_providers.dart';
import 'package:myfschoolse1913/src/features/events/domain/school_event.dart';
import 'package:myfschoolse1913/src/features/home/application/home_providers.dart';
import 'package:myfschoolse1913/src/features/home/domain/home_dashboard.dart';
import 'package:myfschoolse1913/src/features/grades/application/grades_providers.dart';
import 'package:myfschoolse1913/src/features/grades/domain/semester_grades.dart';
import 'package:myfschoolse1913/src/features/system_health/domain/health_check.dart';
import 'package:myfschoolse1913/src/features/system_health/providers/health_providers.dart';

import 'fake_auth_repository.dart';
import 'test_events.dart';
import 'test_grades.dart';
import 'test_home_dashboard.dart';

extension PumpApp on WidgetTester {
  Future<GoRouter> pumpFptSchoolsApp({
    String initialLocation = AppRoutes.login,
    AuthRepository? authRepository,
    HomeDashboard? homeDashboard,
    SemesterGrades? semesterGrades,
    EventsFeed? eventsFeed,
  }) async {
    final router = createAppRouter(initialLocation: initialLocation);
    await pumpWidget(
      ProviderScope(
        overrides: [
          authRepositoryProvider.overrideWithValue(
            authRepository ?? FakeAuthRepository(),
          ),
          homeDashboardProvider.overrideWith(
            (ref) async => homeDashboard ?? testHomeDashboard(),
          ),
          semesterGradesProvider.overrideWith(
            (ref) async => semesterGrades ?? testSemesterGrades(),
          ),
          eventsFeedProvider.overrideWith(
            (ref) async => eventsFeed ?? testEventsFeed(),
          ),
          healthCheckProvider.overrideWith(
            (ref) async => const HealthCheck(status: HealthStatus.up),
          ),
        ],
        child: FptSchoolsApp(router: router),
      ),
    );
    await pumpAndSettle();
    return router;
  }
}
