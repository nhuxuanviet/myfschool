import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/constants/app_colors.dart';
import '../features/auth/application/auth_controller.dart';
import '../features/auth/presentation/login_page.dart';
import '../features/auth/presentation/reset_password_page.dart';
import '../features/clubs/domain/school_club.dart';
import '../features/clubs/presentation/clubs_page.dart';
import '../features/events/domain/school_event.dart';
import '../features/events/presentation/event_details_page.dart';
import '../features/events/presentation/events_page.dart';
import '../features/forms/domain/student_form.dart';
import '../features/forms/presentation/student_form_create_page.dart';
import '../features/forms/presentation/student_form_details_page.dart';
import '../features/forms/presentation/student_forms_page.dart';
import '../features/grades/domain/semester_grades.dart';
import '../features/grades/presentation/grade_details_page.dart';
import '../features/grades/presentation/grades_page.dart';
import '../features/home/presentation/home_page.dart';
import '../features/notifications/presentation/notifications_page.dart';
import '../features/profile/presentation/profile_page.dart';
import '../features/timetable/presentation/timetable_page.dart';
import 'authenticated_shell.dart';

abstract final class AppRoutes {
  static const sessionLoading = '/session-loading';
  static const login = '/login';
  static const resetPassword = '/reset-password';
  static const home = '/home';
  static const schedule = '/schedule';
  static const grades = '/grades';
  static const gradeDetails = '/grades/subjects/:subjectCode';
  static const events = '/events';
  static const eventDetails = '/events/:eventId';
  static const forms = '/forms';
  static const formCreate = '/forms/new';
  static const formDetails = '/forms/:formId';
  static const clubs = '/clubs';
  static const clubDetails = '/clubs/:clubId';
  static const assistant = '/assistant';
  static const notifications = '/notifications';
  static const more = '/more';
}

abstract final class AppRouteNames {
  static const sessionLoading = 'session-loading';
  static const login = 'login';
  static const resetPassword = 'reset-password';
  static const home = 'home';
  static const schedule = 'schedule';
  static const grades = 'grades';
  static const gradeDetails = 'grade-details';
  static const events = 'events';
  static const eventDetails = 'event-details';
  static const forms = 'forms';
  static const formCreate = 'form-create';
  static const formDetails = 'form-details';
  static const clubs = 'clubs';
  static const clubDetails = 'club-details';
  static const assistant = 'assistant';
  static const notifications = 'notifications';
  static const more = 'more';
}

const _shellLocations = [
  AppRoutes.home,
  AppRoutes.schedule,
  AppRoutes.grades,
  AppRoutes.events,
  AppRoutes.more,
];

GoRouter createAppRouter({
  String initialLocation = AppRoutes.login,
  Listenable? refreshListenable,
  GoRouterRedirect? redirect,
}) {
  return GoRouter(
    initialLocation: initialLocation,
    refreshListenable: refreshListenable,
    redirect: redirect,
    routes: [
      GoRoute(
        path: AppRoutes.sessionLoading,
        name: AppRouteNames.sessionLoading,
        builder: (context, state) => const _SessionLoadingPage(),
      ),
      GoRoute(
        path: AppRoutes.login,
        name: AppRouteNames.login,
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: AppRoutes.resetPassword,
        name: AppRouteNames.resetPassword,
        builder: (context, state) => const ResetPasswordPage(),
      ),
      ShellRoute(
        builder: (context, state, child) => AuthenticatedShell(
          location: state.matchedLocation,
          navigationLocations: _shellLocations,
          onDestinationSelected: (index) => context.go(_shellLocations[index]),
          hideNavigation: _shouldHideShellNavigation(state.matchedLocation),
          child: child,
        ),
        routes: [
          GoRoute(
            path: AppRoutes.home,
            name: AppRouteNames.home,
            builder: (context, state) => const HomePage(),
          ),
          GoRoute(
            path: AppRoutes.schedule,
            name: AppRouteNames.schedule,
            builder: (context, state) => const TimetablePage(),
          ),
          GoRoute(
            path: AppRoutes.grades,
            name: AppRouteNames.grades,
            builder: (context, state) => const GradesPage(),
          ),
          GoRoute(
            path: AppRoutes.gradeDetails,
            name: AppRouteNames.gradeDetails,
            redirect: (context, state) {
              final termId = state.uri.queryParameters['termId'];
              return isValidGradeTermId(termId) ? null : AppRoutes.grades;
            },
            builder: (context, state) => GradeDetailsPage(
              subjectCode: state.pathParameters['subjectCode']!,
              termId: state.uri.queryParameters['termId']!,
            ),
          ),
          GoRoute(
            path: AppRoutes.events,
            name: AppRouteNames.events,
            builder: (context, state) => const EventsPage(),
          ),
          GoRoute(
            path: AppRoutes.eventDetails,
            name: AppRouteNames.eventDetails,
            redirect: (context, state) {
              final eventId = state.pathParameters['eventId'];
              final canonicalEventId = canonicalSchoolEventId(eventId);
              if (canonicalEventId == null) return AppRoutes.events;
              return canonicalEventId == eventId
                  ? null
                  : '${AppRoutes.events}/$canonicalEventId';
            },
            builder: (context, state) => EventDetailsPage(
              eventId: canonicalSchoolEventId(state.pathParameters['eventId'])!,
            ),
          ),
          GoRoute(
            path: AppRoutes.forms,
            name: AppRouteNames.forms,
            builder: (context, state) => const StudentFormsPage(),
          ),
          GoRoute(
            path: AppRoutes.formCreate,
            name: AppRouteNames.formCreate,
            builder: (context, state) => const StudentFormCreatePage(),
          ),
          GoRoute(
            path: AppRoutes.formDetails,
            name: AppRouteNames.formDetails,
            redirect: (context, state) {
              final formId = state.pathParameters['formId'];
              final canonical = canonicalStudentFormId(formId);
              if (canonical == null) return AppRoutes.forms;
              return canonical == formId
                  ? null
                  : '${AppRoutes.forms}/$canonical';
            },
            builder: (context, state) => StudentFormDetailsPage(
              formId: canonicalStudentFormId(state.pathParameters['formId'])!,
            ),
          ),
          GoRoute(
            path: AppRoutes.clubs,
            name: AppRouteNames.clubs,
            builder: (context, state) => const ClubsPage(),
          ),
          GoRoute(
            path: AppRoutes.assistant,
            name: AppRouteNames.assistant,
            redirect: (context, state) => AppRoutes.home,
          ),
          GoRoute(
            path: AppRoutes.notifications,
            name: AppRouteNames.notifications,
            builder: (context, state) => const NotificationsPage(),
          ),
          GoRoute(
            path: AppRoutes.clubDetails,
            name: AppRouteNames.clubDetails,
            redirect: (context, state) {
              final clubId = state.pathParameters['clubId'];
              final canonical = canonicalClubId(clubId);
              if (canonical == null) return AppRoutes.clubs;
              return canonical == clubId
                  ? null
                  : '${AppRoutes.clubs}/$canonical';
            },
            builder: (context, state) => ClubDetailsPage(
              clubId: canonicalClubId(state.pathParameters['clubId'])!,
            ),
          ),
          GoRoute(
            path: AppRoutes.more,
            name: AppRouteNames.more,
            builder: (context, state) => const ProfilePage(),
          ),
        ],
      ),
    ],
  );
}

final GoRouter appRouter = createAppRouter();

final appRouterProvider = Provider<GoRouter>((ref) {
  final refresh = _RouterRefreshNotifier();
  ref.listen(authControllerProvider, (_, _) => refresh.notify());
  final router = createAppRouter(
    initialLocation: AppRoutes.sessionLoading,
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final location = state.matchedLocation;
      final isPublicRoute = _isPublicRoute(location);

      if (auth.isLoading) {
        return location == AppRoutes.sessionLoading
            ? null
            : AppRoutes.sessionLoading;
      }

      final authState = switch (auth) {
        AsyncData(:final value) => value,
        _ => const AuthState(),
      };
      if (authState.isAuthenticated) {
        return isPublicRoute ? AppRoutes.home : null;
      }
      return isPublicRoute && location != AppRoutes.sessionLoading
          ? null
          : AppRoutes.login;
    },
  );
  ref.onDispose(() {
    router.dispose();
    refresh.dispose();
  });
  return router;
});

bool _isPublicRoute(String location) {
  return location == AppRoutes.sessionLoading ||
      location == AppRoutes.login ||
      location == AppRoutes.resetPassword;
}

bool _shouldHideShellNavigation(String location) {
  return location == AppRoutes.notifications ||
      location == AppRoutes.formCreate ||
      location.startsWith('${AppRoutes.forms}/') ||
      location.startsWith('${AppRoutes.events}/') ||
      location.startsWith('${AppRoutes.clubs}/') ||
      location.startsWith('${AppRoutes.grades}/');
}

final class _RouterRefreshNotifier extends ChangeNotifier {
  void notify() => notifyListeners();
}

class _SessionLoadingPage extends StatelessWidget {
  const _SessionLoadingPage();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Semantics(
          label: 'Đang khôi phục phiên đăng nhập',
          liveRegion: true,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              CircularProgressIndicator(color: AppColors.primary),
              SizedBox(height: 16),
              Text('Đang tải...', style: TextStyle(color: AppColors.mutedText)),
            ],
          ),
        ),
      ),
    );
  }
}
