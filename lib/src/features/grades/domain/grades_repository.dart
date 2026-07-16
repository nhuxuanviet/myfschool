import 'semester_grades.dart';

abstract interface class GradesRepository {
  /// Omitting [termId] asks the server to choose the student's latest term.
  Future<SemesterGrades> getGrades({String? termId});
}
