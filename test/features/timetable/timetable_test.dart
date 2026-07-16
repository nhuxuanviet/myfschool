import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/timetable/domain/timetable.dart';

import '../../helpers/test_timetable.dart';

void main() {
  test('parses a complete seven-day school timetable', () {
    final timetable = Timetable.fromJson(testTimetableJson());

    expect(timetable.timeZone, 'Asia/Ho_Chi_Minh');
    expect(formatTimetableDate(timetable.weekStart), '2026-07-13');
    expect(formatTimetableDate(timetable.weekEnd), '2026-07-19');
    expect(timetable.academicTerms.single.name, 'Học kỳ I');
    expect(timetable.days, hasLength(7));
    expect(timetable.days[2].dayOfWeek, TimetableDayOfWeek.wednesday);
    expect(
      timetable.days[2].lessons.last.status,
      TimetableLessonStatus.cancelled,
    );
    expect(
      timetable.days[5].lessons.single.status,
      TimetableLessonStatus.added,
    );
  });

  test('accepts a timetable with no active academic terms', () {
    final timetable = Timetable.fromJson(
      testTimetableJson(
        academicTerms: const [],
        lessonsByDay: testEmptyLessonsByDay(),
      ),
    );

    expect(timetable.academicTerms, isEmpty);
  });

  test('requires the non-null academicTerms list to be present', () {
    final json = testTimetableJson()..remove('academicTerms');

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects academic terms that are not chronologically ordered', () {
    final json = testTimetableJson(
      academicTerms: [
        testAcademicTermJson(
          code: 'SEMESTER_2',
          name: 'Học kỳ II',
          startsOn: '2026-07-14',
          endsOn: '2026-07-19',
        ),
        testAcademicTermJson(
          code: 'SEMESTER_1',
          name: 'Học kỳ I',
          startsOn: '2026-07-13',
          endsOn: '2026-07-15',
        ),
      ],
    );

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('orders same-start academic terms by end date then code', () {
    final endDateOutOfOrder = testTimetableJson(
      academicTerms: [
        testAcademicTermJson(code: 'A', endsOn: '2026-07-19'),
        testAcademicTermJson(code: 'B', endsOn: '2026-07-18'),
      ],
    );
    final codeOutOfOrder = testTimetableJson(
      academicTerms: [
        testAcademicTermJson(code: 'B'),
        testAcademicTermJson(code: 'A'),
      ],
    );

    expect(
      () => Timetable.fromJson(endDateOutOfOrder),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => Timetable.fromJson(codeOutOfOrder),
      throwsA(isA<FormatException>()),
    );
  });

  test('rejects an academic term outside the requested week', () {
    final json = testTimetableJson(
      academicTerms: [
        testAcademicTermJson(startsOn: '2026-07-20', endsOn: '2026-12-31'),
      ],
    );

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects lessons when no academic term is returned', () {
    final json = testTimetableJson(academicTerms: const []);

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects a lesson outside every returned academic term', () {
    final json = testTimetableJson(
      academicTerms: [
        testAcademicTermJson(startsOn: '2026-07-13', endsOn: '2026-07-15'),
      ],
    );

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects a non-Monday week start', () {
    final json = testTimetableJson()..['weekStart'] = '2026-07-07';

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects a lesson whose configured duration is not 45 minutes', () {
    final json = testTimetableJson();
    final days = json['days'] as List<dynamic>;
    final monday = days.first as Map<String, dynamic>;
    final lessons = monday['lessons'] as List<dynamic>;
    (lessons.first as Map<String, dynamic>)['endTime'] = '08:00';

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });

  test('rejects a blank optional lesson field when it is present', () {
    final json = testTimetableJson();
    final days = json['days'] as List<dynamic>;
    final monday = days.first as Map<String, dynamic>;
    final lessons = monday['lessons'] as List<dynamic>;
    (lessons.first as Map<String, dynamic>)['room'] = '  ';

    expect(() => Timetable.fromJson(json), throwsA(isA<FormatException>()));
  });
}
