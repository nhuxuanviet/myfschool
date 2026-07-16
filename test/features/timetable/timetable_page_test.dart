import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/timetable/application/timetable_providers.dart';
import 'package:myfschoolse1913/src/features/timetable/domain/timetable.dart';
import 'package:myfschoolse1913/src/features/timetable/domain/timetable_repository.dart';
import 'package:myfschoolse1913/src/features/timetable/presentation/timetable_page.dart';

import '../../helpers/test_timetable.dart';

void main() {
  testWidgets('renders the timetable route and switches the selected day', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeTimetableRepository(
      initial: testTimetable(),
      weeks: {'2026-07-20': testTimetable(weekStart: '2026-07-20')},
    );
    final router = await _pumpTimetable(tester, repository);
    addTearDown(router.dispose);

    expect(find.byType(TimetablePage), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);
    expect(find.bySemanticsLabel('Lịch học'), findsOneWidget);
    expect(
      find.bySemanticsLabel('Tuần 2026-07-13 đến 2026-07-19'),
      findsOneWidget,
    );
    expect(find.bySemanticsLabel('Lịch ngày 2026-07-13'), findsOneWidget);
    expect(
      find.bySemanticsLabel(
        RegExp(r'Tiết 1: Toán học\. 07:00 - 07:45\. Theo lịch'),
      ),
      findsOneWidget,
    );

    await tester.tap(find.bySemanticsLabel('Chọn ngày 2026-07-15'));
    await tester.pumpAndSettle();

    expect(find.bySemanticsLabel('Lịch ngày 2026-07-15'), findsOneWidget);
    expect(
      find.bySemanticsLabel(
        RegExp(r'Hóa học.*Đã hủy.*Nghỉ để tham gia hoạt động toàn trường'),
      ),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('derives adjacent week requests from the returned week start', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeTimetableRepository(
      initial: testTimetable(),
      weeks: {'2026-07-20': testTimetable(weekStart: '2026-07-20')},
    );
    final router = await _pumpTimetable(tester, repository);
    addTearDown(router.dispose);

    expect(repository.requestedWeeks, [null]);
    await tester.tap(find.bySemanticsLabel('Tuần sau'));
    await tester.pumpAndSettle();

    expect(repository.requestedWeeks, [null, DateTime.utc(2026, 7, 20)]);
    expect(
      find.bySemanticsLabel('Tuần 2026-07-20 đến 2026-07-26'),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('shows empty-term and empty-day states', (tester) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeTimetableRepository(
      initial: testTimetable(
        academicTerms: const [],
        lessonsByDay: testEmptyLessonsByDay(),
      ),
    );
    final router = await _pumpTimetable(tester, repository);
    addTearDown(router.dispose);

    expect(find.text('Chưa có học kỳ đang hoạt động.'), findsOneWidget);
    expect(
      find.bySemanticsLabel(RegExp('Không có tiết học trong ngày này')),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('shows all transition terms without hiding the day lessons', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeTimetableRepository(
      initial: testTimetable(
        academicTerms: [
          testAcademicTermJson(
            code: 'SEMESTER_1',
            name: 'Học kỳ I',
            startsOn: '2026-07-13',
            endsOn: '2026-07-15',
          ),
          testAcademicTermJson(
            code: 'SEMESTER_2',
            name: 'Học kỳ II',
            startsOn: '2026-07-16',
            endsOn: '2026-07-19',
          ),
        ],
      ),
    );
    final router = await _pumpTimetable(tester, repository);
    addTearDown(router.dispose);

    expect(find.text('Chuyển tiếp học kỳ trong tuần này'), findsOneWidget);
    expect(
      find.text('Học kỳ I • 2026-2027\n13/07/2026 – 15/07/2026'),
      findsOneWidget,
    );
    expect(
      find.text('Học kỳ II • 2026-2027\n16/07/2026 – 19/07/2026'),
      findsOneWidget,
    );
    expect(
      find.bySemanticsLabel(RegExp(r'Tiết 1: Toán học\. 07:00 - 07:45\.')),
      findsOneWidget,
    );
    await tester.tap(find.bySemanticsLabel('Chọn ngày 2026-07-17'));
    await tester.pumpAndSettle();
    expect(
      find.bySemanticsLabel(RegExp(r'Tiết 1: Vật lý\. 13:00 - 13:45\.')),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('shows loading then retries a failed timetable request', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final pending = Completer<Timetable>();
    var requests = 0;
    final repository = _FakeTimetableRepository.withHandler((_) {
      requests += 1;
      return requests == 1
          ? pending.future
          : Future<Timetable>.value(testTimetable());
    });
    final router = await _pumpTimetable(tester, repository, settle: false);
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel('Đang tải lịch học'), findsOneWidget);
    pending.completeError(StateError('offline'));
    await tester.pump();
    expect(find.bySemanticsLabel('Không thể tải lịch học'), findsOneWidget);

    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.bySemanticsLabel('Lịch ngày 2026-07-13'), findsOneWidget);
    expect(requests, 2);
    semantics.dispose();
  });
}

Future<GoRouter> _pumpTimetable(
  WidgetTester tester,
  TimetableRepository repository, {
  bool settle = true,
}) async {
  final router = createAppRouter(initialLocation: AppRoutes.schedule);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [timetableRepositoryProvider.overrideWithValue(repository)],
      child: FptSchoolsApp(router: router),
    ),
  );
  if (settle) await tester.pumpAndSettle();
  return router;
}

final class _FakeTimetableRepository implements TimetableRepository {
  _FakeTimetableRepository({required this.initial, this.weeks = const {}})
    : _handler = null;

  _FakeTimetableRepository.withHandler(
    Future<Timetable> Function(DateTime? weekStart) handler,
  ) : initial = null,
      weeks = const {},
      _handler = handler;

  final Timetable? initial;
  final Map<String, Timetable> weeks;
  final Future<Timetable> Function(DateTime? weekStart)? _handler;
  final List<DateTime?> requestedWeeks = [];

  @override
  Future<Timetable> getTimetable({DateTime? weekStart}) {
    requestedWeeks.add(weekStart);
    if (_handler case final handler?) return handler(weekStart);
    if (weekStart == null) return Future.value(initial!);
    return Future.value(weeks[formatTimetableDate(weekStart)] ?? initial!);
  }
}
