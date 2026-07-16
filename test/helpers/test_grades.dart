import 'package:myfschoolse1913/src/features/grades/domain/semester_grades.dart';

const testCurrentTermId = '11111111-1111-4111-8111-111111111111';
const testPreviousTermId = '22222222-2222-4222-8222-222222222222';

SemesterGrades testSemesterGrades({
  Map<String, dynamic>? selectedTerm,
  List<Map<String, dynamic>>? availableTerms,
  List<Map<String, dynamic>>? subjects,
}) {
  return SemesterGrades.fromJson(
    testSemesterGradesJson(
      selectedTerm: selectedTerm,
      availableTerms: availableTerms,
      subjects: subjects,
    ),
  );
}

Map<String, dynamic> testSemesterGradesJson({
  Map<String, dynamic>? selectedTerm,
  List<Map<String, dynamic>>? availableTerms,
  List<Map<String, dynamic>>? subjects,
}) {
  final currentTerm = selectedTerm ?? testGradeTermJson();
  return {
    'timeZone': 'Asia/Ho_Chi_Minh',
    'selectedTerm': currentTerm,
    'availableTerms':
        availableTerms ??
        [
          testGradeTermJson(),
          testGradeTermJson(
            id: testPreviousTermId,
            academicYear: '2025-2026',
            code: 'SEMESTER_2',
            name: 'Học kỳ II',
            startsOn: '2026-01-19',
            endsOn: '2026-05-30',
          ),
        ],
    'subjects':
        subjects ??
        [
          testNumericSubjectJson(),
          testRemarkSubjectJson(),
          testIncompleteNumericSubjectJson(),
        ],
  };
}

Map<String, dynamic> testGradeTermJson({
  String id = testCurrentTermId,
  String academicYear = '2026-2027',
  String code = 'SEMESTER_1',
  String name = 'Học kỳ I',
  String startsOn = '2026-07-13',
  String endsOn = '2026-12-31',
}) {
  return {
    'id': id,
    'academicYear': academicYear,
    'code': code,
    'name': name,
    'startsOn': startsOn,
    'endsOn': endsOn,
  };
}

Map<String, dynamic> testNumericSubjectJson({
  String code = 'MATH',
  String name = 'Toán học',
  double? termAverage = 8.4,
  List<Map<String, dynamic>>? assessments,
}) {
  return {
    'code': code,
    'name': name,
    'assessmentMode': 'NUMERIC',
    'annualLessonCount': 105,
    'requiredRegularAssessments': 4,
    'termAverage': termAverage,
    'termResult': null,
    'assessments':
        assessments ??
        [
          testGradeAssessmentJson(
            kind: 'REGULAR',
            form: 'ORAL',
            displayLabel: 'Miệng',
            score: 9,
            assessedOn: '2026-08-05',
          ),
          testGradeAssessmentJson(
            kind: 'REGULAR',
            form: 'WRITTEN',
            displayLabel: '15 phút',
            durationMinutes: 15,
            score: 8,
            assessedOn: '2026-09-12',
          ),
          testGradeAssessmentJson(
            kind: 'REGULAR',
            form: 'WRITTEN',
            displayLabel: '45 phút',
            durationMinutes: 45,
            score: 8.5,
            assessedOn: '2026-10-03',
          ),
          testGradeAssessmentJson(
            kind: 'REGULAR',
            form: 'PRESENTATION',
            displayLabel: 'Thuyết trình nhóm',
            score: 8,
            assessedOn: '2026-10-17',
          ),
          testGradeAssessmentJson(
            kind: 'MIDTERM',
            form: 'WRITTEN',
            displayLabel: 'Kiểm tra giữa kỳ',
            durationMinutes: 45,
            score: 8.5,
            assessedOn: '2026-10-28',
          ),
          testGradeAssessmentJson(
            kind: 'FINAL',
            form: 'WRITTEN',
            displayLabel: 'Thi cuối kỳ',
            durationMinutes: 90,
            score: 8.5,
            assessedOn: '2026-12-20',
          ),
        ],
  };
}

Map<String, dynamic> testRemarkSubjectJson({
  String code = 'PE',
  String name = 'Giáo dục thể chất',
  String termResult = 'ACHIEVED',
}) {
  return {
    'code': code,
    'name': name,
    'assessmentMode': 'REMARK',
    'annualLessonCount': null,
    'requiredRegularAssessments': 2,
    'termAverage': null,
    'termResult': termResult,
    'assessments': [
      testGradeAssessmentJson(
        kind: 'REGULAR',
        form: 'PRACTICAL',
        displayLabel: 'Thực hành',
        score: null,
        outcome: 'ACHIEVED',
        assessedOn: '2026-08-11',
      ),
      testGradeAssessmentJson(
        kind: 'REGULAR',
        form: 'PRACTICAL',
        displayLabel: 'Kiểm tra thực hành',
        score: null,
        outcome: 'ACHIEVED',
        assessedOn: '2026-09-22',
      ),
      testGradeAssessmentJson(
        kind: 'MIDTERM',
        form: 'PRACTICAL',
        displayLabel: 'Đánh giá giữa kỳ',
        score: null,
        outcome: 'ACHIEVED',
        assessedOn: '2026-10-29',
      ),
      testGradeAssessmentJson(
        kind: 'FINAL',
        form: 'PRACTICAL',
        displayLabel: 'Đánh giá cuối kỳ',
        score: null,
        outcome: 'ACHIEVED',
        assessedOn: '2026-12-16',
      ),
    ],
  };
}

Map<String, dynamic> testIncompleteNumericSubjectJson() {
  return {
    'code': 'BIO',
    'name': 'Sinh học',
    'assessmentMode': 'NUMERIC',
    'annualLessonCount': 35,
    'requiredRegularAssessments': 2,
    'termAverage': null,
    'termResult': null,
    'assessments': [
      testGradeAssessmentJson(
        kind: 'REGULAR',
        form: 'ORAL',
        displayLabel: 'Miệng',
        score: 8,
        assessedOn: '2026-08-19',
      ),
      testGradeAssessmentJson(
        kind: 'REGULAR',
        form: 'WRITTEN',
        displayLabel: '15 phút',
        durationMinutes: 15,
        status: 'MAKE_UP_REQUIRED',
        score: null,
        assessedOn: null,
      ),
    ],
  };
}

Map<String, dynamic> testGradeAssessmentJson({
  String kind = 'REGULAR',
  String form = 'ORAL',
  String displayLabel = 'Miệng',
  int? durationMinutes,
  String status = 'RECORDED',
  num? score = 8,
  String? outcome,
  String? assessedOn = '2026-08-05',
}) {
  return {
    'kind': kind,
    'form': form,
    'displayLabel': displayLabel,
    'durationMinutes': durationMinutes,
    'status': status,
    'score': score,
    'outcome': outcome,
    'assessedOn': assessedOn,
  };
}
