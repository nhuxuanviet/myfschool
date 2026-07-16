import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/timetable_api_repository.dart';
import '../domain/timetable.dart';
import '../domain/timetable_repository.dart';

final timetableRepositoryProvider = Provider<TimetableRepository>(
  (ref) => ApiTimetableRepository(ref.watch(authenticatedApiClientProvider)),
);

/// `null` deliberately maps to an API request without `weekStart`, letting the
/// server determine the current academic week for the logged-in student.
final timetableWeekStartProvider =
    NotifierProvider.autoDispose<_TimetableWeekStartNotifier, DateTime?>(
      _TimetableWeekStartNotifier.new,
    );

final class _TimetableWeekStartNotifier extends Notifier<DateTime?> {
  @override
  DateTime? build() => null;

  void select(DateTime? weekStart) {
    state = weekStart;
  }
}

final timetableSelectedDateProvider =
    NotifierProvider.autoDispose<_TimetableSelectedDateNotifier, DateTime?>(
      _TimetableSelectedDateNotifier.new,
    );

final class _TimetableSelectedDateNotifier extends Notifier<DateTime?> {
  @override
  DateTime? build() => null;

  void select(DateTime? date) {
    state = date;
  }
}

final timetableProvider = FutureProvider.autoDispose<Timetable>((ref) {
  final weekStart = ref.watch(timetableWeekStartProvider);
  return ref
      .watch(timetableRepositoryProvider)
      .getTimetable(weekStart: weekStart);
});
