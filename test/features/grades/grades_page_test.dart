import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/grades/application/grades_providers.dart';
import 'package:myfschoolse1913/src/features/grades/domain/grades_repository.dart';
import 'package:myfschoolse1913/src/features/grades/domain/semester_grades.dart';
import 'package:myfschoolse1913/src/features/grades/presentation/grade_details_page.dart';
import 'package:myfschoolse1913/src/features/grades/presentation/grades_page.dart';

import '../../helpers/test_grades.dart';

void main() {
  testWidgets('selects another term through accessible controls', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final previousTerm = _previousTermJson();
    final repository = _FakeGradesRepository(
      initial: testSemesterGrades(),
      gradesByTerm: {
        testPreviousTermId: testSemesterGrades(
          selectedTerm: previousTerm,
          availableTerms: [testGradeTermJson(), previousTerm],
          subjects: [testNumericSubjectJson(termAverage: 7.6)],
        ),
      },
    );
    final router = await _pumpGrades(tester, repository);
    addTearDown(router.dispose);

    expect(find.byType(GradesPage), findsOneWidget);
    expect(find.bySemanticsLabel('Điểm học kỳ'), findsOneWidget);
    expect(
      find.bySemanticsLabel('Chọn học kỳ Học kỳ I, 2026-2027'),
      findsOneWidget,
    );

    await tester.tap(find.bySemanticsLabel('Chọn học kỳ Học kỳ I, 2026-2027'));
    await tester.pumpAndSettle();
    expect(find.bySemanticsLabel('Chọn Học kỳ II, 2025-2026'), findsOneWidget);

    await tester.tap(find.bySemanticsLabel('Chọn Học kỳ II, 2025-2026'));
    await tester.pumpAndSettle();

    expect(repository.requestedTermIds, [null, testPreviousTermId]);
    expect(
      find.bySemanticsLabel('Chọn học kỳ Học kỳ II, 2025-2026'),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('opens accessible subject assessment details', (tester) async {
    final semantics = tester.ensureSemantics();
    final router = await _pumpGrades(
      tester,
      _FakeGradesRepository(initial: testSemesterGrades()),
    );
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel('Xem chi tiết Toán học'), findsOneWidget);
    await tester.tap(find.bySemanticsLabel('Xem chi tiết Toán học'));
    await tester.pumpAndSettle();

    expect(
      router.state.uri,
      Uri(
        path: '/grades/subjects/MATH',
        queryParameters: {'termId': testCurrentTermId},
      ),
    );
    expect(find.byType(GradeDetailsPage), findsOneWidget);
    expect(find.bySemanticsLabel('Chi tiết điểm Toán học'), findsOneWidget);
    expect(
      find.bySemanticsLabel('Đánh giá thường xuyên', skipOffstage: false),
      findsOneWidget,
    );
    expect(
      find.bySemanticsLabel('Giữa kỳ', skipOffstage: false),
      findsOneWidget,
    );
    expect(
      find.bySemanticsLabel('Cuối kỳ', skipOffstage: false),
      findsOneWidget,
    );
    expect(find.text('Miệng'), findsOneWidget);
    expect(find.text('9'), findsWidgets);
    expect(find.text('Điểm'), findsNothing);
    expect(find.text('15 phút'), findsOneWidget);
    expect(
      find.bySemanticsLabel(
        RegExp(
          r'15 phút\. Hình thức: Viết\. Điểm: 8\. Đã ghi nhận\. '
          r'Thời lượng: 15 phút\. Ngày đánh giá 2026-09-12',
        ),
      ),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('loads the requested historical term from a details deep link', (
    tester,
  ) async {
    final historicalTerm = _previousTermJson();
    final historicalGrades = testSemesterGrades(
      selectedTerm: historicalTerm,
      availableTerms: [testGradeTermJson(), historicalTerm],
      subjects: [
        testNumericSubjectJson(
          name: 'Toán học học kỳ II',
          termAverage: 6.5,
          assessments: [
            testGradeAssessmentJson(
              kind: 'REGULAR',
              form: 'WRITTEN',
              displayLabel: '45 phút học kỳ II',
              durationMinutes: 45,
              score: 6.5,
              assessedOn: '2026-04-12',
            ),
          ],
        ),
      ],
    );
    final repository = _FakeGradesRepository(
      initial: testSemesterGrades(),
      gradesByTerm: {testPreviousTermId: historicalGrades},
    );
    final router = await _pumpGrades(
      tester,
      repository,
      initialLocation: '/grades/subjects/MATH?termId=$testPreviousTermId',
    );
    addTearDown(router.dispose);

    expect(repository.requestedTermIds, [testPreviousTermId]);
    expect(find.byType(GradeDetailsPage), findsOneWidget);
    expect(
      find.bySemanticsLabel('Chi tiết điểm Toán học học kỳ II'),
      findsOneWidget,
    );
    expect(find.text('Điểm trung bình học kỳ'), findsOneWidget);
    expect(find.text('6,5'), findsNWidgets(2));
    expect(find.text('45 phút học kỳ II'), findsOneWidget);
  });

  testWidgets(
    'returns safely to grades when a details term ID is missing or invalid',
    (tester) async {
      final repository = _FakeGradesRepository(initial: testSemesterGrades());
      final router = await _pumpGrades(
        tester,
        repository,
        initialLocation: '/grades/subjects/MATH',
      );
      addTearDown(router.dispose);

      expect(find.byType(GradesPage), findsOneWidget);
      expect(find.byType(GradeDetailsPage), findsNothing);
      expect(router.state.uri.path, AppRoutes.grades);

      router.go('/grades/subjects/MATH?termId=not-a-uuid');
      await tester.pumpAndSettle();

      expect(find.byType(GradesPage), findsOneWidget);
      expect(find.byType(GradeDetailsPage), findsNothing);
      expect(router.state.uri.path, AppRoutes.grades);
    },
  );

  testWidgets('does not render a numerical average for incomplete results', (
    tester,
  ) async {
    final router = await _pumpGrades(
      tester,
      _FakeGradesRepository(initial: testSemesterGrades()),
    );
    addTearDown(router.dispose);

    expect(find.text('Chưa có điểm'), findsOneWidget);
    expect(find.text('Điểm trung bình học kỳ: 0'), findsNothing);
  });

  testWidgets('shows loading then retries a failed grades request', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final pending = Completer<SemesterGrades>();
    var requests = 0;
    final repository = _FakeGradesRepository.withHandler((_) {
      requests += 1;
      return requests == 1
          ? pending.future
          : Future<SemesterGrades>.value(testSemesterGrades());
    });
    final router = await _pumpGrades(tester, repository, settle: false);
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel('Đang tải điểm học kỳ'), findsOneWidget);
    pending.completeError(StateError('offline'));
    await tester.pump();
    expect(find.bySemanticsLabel('Không thể tải điểm học kỳ'), findsOneWidget);

    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.bySemanticsLabel('Xem chi tiết Toán học'), findsOneWidget);
    expect(requests, 2);
    semantics.dispose();
  });
}

Future<GoRouter> _pumpGrades(
  WidgetTester tester,
  GradesRepository repository, {
  bool settle = true,
  String initialLocation = AppRoutes.grades,
}) async {
  final router = createAppRouter(initialLocation: initialLocation);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [gradesRepositoryProvider.overrideWithValue(repository)],
      child: FptSchoolsApp(router: router),
    ),
  );
  if (settle) await tester.pumpAndSettle();
  return router;
}

Map<String, dynamic> _previousTermJson() {
  return testGradeTermJson(
    id: testPreviousTermId,
    academicYear: '2025-2026',
    code: 'SEMESTER_2',
    name: 'Học kỳ II',
    startsOn: '2026-01-19',
    endsOn: '2026-05-30',
  );
}

final class _FakeGradesRepository implements GradesRepository {
  _FakeGradesRepository({required this.initial, this.gradesByTerm = const {}})
    : _handler = null;

  _FakeGradesRepository.withHandler(
    Future<SemesterGrades> Function(String? termId) handler,
  ) : initial = null,
      gradesByTerm = const {},
      _handler = handler;

  final SemesterGrades? initial;
  final Map<String, SemesterGrades> gradesByTerm;
  final Future<SemesterGrades> Function(String? termId)? _handler;
  final List<String?> requestedTermIds = [];

  @override
  Future<SemesterGrades> getGrades({String? termId}) {
    requestedTermIds.add(termId);
    if (_handler case final handler?) return handler(termId);
    if (termId == null) return Future.value(initial!);
    return Future.value(gradesByTerm[termId] ?? initial!);
  }
}
