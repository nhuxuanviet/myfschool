import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/grades_api_repository.dart';
import '../domain/grades_repository.dart';
import '../domain/semester_grades.dart';

final gradesRepositoryProvider = Provider<GradesRepository>(
  (ref) => ApiGradesRepository(ref.watch(authenticatedApiClientProvider)),
);

/// `null` maps to an API request without `termId`, allowing the server to
/// select the student's newest available academic term.
final selectedGradeTermIdProvider =
    NotifierProvider.autoDispose<_SelectedGradeTermIdNotifier, String?>(
      _SelectedGradeTermIdNotifier.new,
    );

final class _SelectedGradeTermIdNotifier extends Notifier<String?> {
  @override
  String? build() => null;

  void select(String? termId) {
    state = termId;
  }
}

final semesterGradesProvider = FutureProvider.autoDispose<SemesterGrades>((
  ref,
) {
  final termId = ref.watch(selectedGradeTermIdProvider);
  return ref.watch(gradesRepositoryProvider).getGrades(termId: termId);
});

/// Loads a grade report for the term encoded in a details deep link.
///
/// It deliberately does not depend on [selectedGradeTermIdProvider], which is
/// transient UI state and is absent when a student opens a shared or restored
/// details URL directly.
final semesterGradesForTermProvider = FutureProvider.autoDispose
    .family<SemesterGrades, String>((ref, termId) async {
      final grades = await ref
          .watch(gradesRepositoryProvider)
          .getGrades(termId: termId);
      if (grades.selectedTerm.id != termId) {
        throw const FormatException(
          'The grades response did not match the requested academic term.',
        );
      }
      return grades;
    });
