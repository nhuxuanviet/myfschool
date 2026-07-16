import 'timetable.dart';

abstract interface class TimetableRepository {
  /// Omitting [weekStart] asks the server for the student's current school week.
  Future<Timetable> getTimetable({DateTime? weekStart});
}
