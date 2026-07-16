import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/grades/domain/semester_grades.dart';

import '../../helpers/test_grades.dart';

void main() {
  test('recognizes the UUID format used by grade-term deep links', () {
    expect(isValidGradeTermId(testCurrentTermId), isTrue);
    expect(isValidGradeTermId('not-a-uuid'), isFalse);
    expect(isValidGradeTermId(null), isFalse);
  });

  test(
    'parses the semester grades contract with numeric and remark subjects',
    () {
      final grades = SemesterGrades.fromJson(testSemesterGradesJson());

      expect(grades.timeZone, 'Asia/Ho_Chi_Minh');
      expect(grades.selectedTerm.id, testCurrentTermId);
      expect(grades.availableTerms, hasLength(2));
      expect(grades.subjects, hasLength(3));
      expect(grades.subjects.first.assessmentMode, AssessmentMode.numeric);
      expect(grades.subjects.first.termAverage, 8.4);
      expect(grades.subjects[1].assessmentMode, AssessmentMode.remark);
      expect(grades.subjects[1].termResult, TermResult.achieved);
      expect(grades.subjects.first.assessments[1].label, '15 phút');
      expect(
        grades.subjects.last.assessments.last.status,
        AssessmentStatus.makeUpRequired,
      );
    },
  );

  test('requires a nonblank display label for every assessment', () {
    final json = testSemesterGradesJson();
    final subjects = json['subjects'] as List<dynamic>;
    final subject = subjects.first as Map<String, dynamic>;
    final assessments = subject['assessments'] as List<dynamic>;
    (assessments.first as Map<String, dynamic>)['displayLabel'] = ' ';

    expect(
      () => SemesterGrades.fromJson(json),
      throwsA(isA<FormatException>()),
    );
  });

  test(
    'rejects a result that conflicts with the assessment mode and status',
    () {
      final json = testSemesterGradesJson();
      final subjects = json['subjects'] as List<dynamic>;
      final remarkSubject = subjects[1] as Map<String, dynamic>;
      final assessments = remarkSubject['assessments'] as List<dynamic>;
      (assessments.first as Map<String, dynamic>)['score'] = 7;

      expect(
        () => SemesterGrades.fromJson(json),
        throwsA(isA<FormatException>()),
      );
    },
  );

  test(
    'rejects scores and durations outside the strict assessment contract',
    () {
      final scoreJson = testSemesterGradesJson();
      final scoreSubjects = scoreJson['subjects'] as List<dynamic>;
      final scoreSubject = scoreSubjects.first as Map<String, dynamic>;
      final scoreAssessments = scoreSubject['assessments'] as List<dynamic>;
      (scoreAssessments.first as Map<String, dynamic>)['score'] = 8.25;

      final durationJson = testSemesterGradesJson();
      final durationSubjects = durationJson['subjects'] as List<dynamic>;
      final durationSubject = durationSubjects.first as Map<String, dynamic>;
      final durationAssessments =
          durationSubject['assessments'] as List<dynamic>;
      (durationAssessments.first as Map<String, dynamic>)['durationMinutes'] =
          181;

      expect(
        () => SemesterGrades.fromJson(scoreJson),
        throwsA(isA<FormatException>()),
      );
      expect(
        () => SemesterGrades.fromJson(durationJson),
        throwsA(isA<FormatException>()),
      );
    },
  );

  test('rejects terms that are not returned newest first', () {
    final olderTerm = testGradeTermJson(
      id: testPreviousTermId,
      academicYear: '2025-2026',
      code: 'SEMESTER_2',
      name: 'Học kỳ II',
      startsOn: '2026-01-19',
      endsOn: '2026-05-30',
    );
    final json = testSemesterGradesJson(
      selectedTerm: olderTerm,
      availableTerms: [olderTerm, testGradeTermJson()],
    );

    expect(
      () => SemesterGrades.fromJson(json),
      throwsA(isA<FormatException>()),
    );
  });

  test('rejects a selected term that is not in the available term list', () {
    final json = testSemesterGradesJson(
      availableTerms: [
        testGradeTermJson(
          id: testPreviousTermId,
          academicYear: '2025-2026',
          code: 'SEMESTER_2',
          name: 'Học kỳ II',
          startsOn: '2026-01-19',
          endsOn: '2026-05-30',
        ),
      ],
    );

    expect(
      () => SemesterGrades.fromJson(json),
      throwsA(isA<FormatException>()),
    );
  });

  test('rejects a selected term whose immutable fields do not match', () {
    final json = testSemesterGradesJson(
      availableTerms: [
        testGradeTermJson(name: 'Học kỳ I không khớp'),
        testGradeTermJson(
          id: testPreviousTermId,
          academicYear: '2025-2026',
          code: 'SEMESTER_2',
          name: 'Học kỳ II',
          startsOn: '2026-01-19',
          endsOn: '2026-05-30',
        ),
      ],
    );

    expect(
      () => SemesterGrades.fromJson(json),
      throwsA(isA<FormatException>()),
    );
  });

  test(
    'enforces the numeric regular-assessment requirement by annual lessons',
    () {
      final json = testSemesterGradesJson();
      final subjects = json['subjects'] as List<dynamic>;
      final numericSubject = subjects.first as Map<String, dynamic>;
      numericSubject['annualLessonCount'] = 36;
      numericSubject['requiredRegularAssessments'] = 2;

      expect(
        () => SemesterGrades.fromJson(json),
        throwsA(isA<FormatException>()),
      );
    },
  );

  test('requires the remark-subject configuration from the API contract', () {
    final json = testSemesterGradesJson();
    final subjects = json['subjects'] as List<dynamic>;
    final remarkSubject = subjects[1] as Map<String, dynamic>;
    remarkSubject['annualLessonCount'] = 35;

    expect(
      () => SemesterGrades.fromJson(json),
      throwsA(isA<FormatException>()),
    );
  });

  test('uses end date, academic year, and code to order tied term starts', () {
    final firstTerm = testGradeTermJson(
      id: testCurrentTermId,
      code: 'A',
      startsOn: '2026-07-13',
      endsOn: '2026-12-31',
    );
    final secondTerm = testGradeTermJson(
      id: testPreviousTermId,
      code: 'B',
      startsOn: '2026-07-13',
      endsOn: '2026-12-31',
    );
    final json = testSemesterGradesJson(
      selectedTerm: firstTerm,
      availableTerms: [firstTerm, secondTerm],
    );

    expect(
      () => SemesterGrades.fromJson(json),
      throwsA(isA<FormatException>()),
    );
  });
}
