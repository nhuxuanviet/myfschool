import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/grades/data/grades_api_repository.dart';

import '../../helpers/test_grades.dart';

void main() {
  test('loads the newest grades without a termId query parameter', () async {
    late RequestOptions requestedOptions;
    final dio = _dioReturning(testSemesterGradesJson(), (options) {
      requestedOptions = options;
    });

    final grades = await ApiGradesRepository(ApiClient(dio)).getGrades();

    expect(requestedOptions.path, ApiGradesRepository.gradesPath);
    expect(requestedOptions.queryParameters, isEmpty);
    expect(grades.selectedTerm.id, testCurrentTermId);
  });

  test('sends the requested academic term ID unchanged', () async {
    late RequestOptions requestedOptions;
    final previousTerm = testGradeTermJson(
      id: testPreviousTermId,
      academicYear: '2025-2026',
      code: 'SEMESTER_2',
      name: 'Học kỳ II',
      startsOn: '2026-01-19',
      endsOn: '2026-05-30',
    );
    final dio = _dioReturning(
      testSemesterGradesJson(
        selectedTerm: previousTerm,
        availableTerms: [testGradeTermJson(), previousTerm],
      ),
      (options) => requestedOptions = options,
    );

    await ApiGradesRepository(
      ApiClient(dio),
    ).getGrades(termId: testPreviousTermId);

    expect(
      requestedOptions.queryParameters,
      equals(const {'termId': testPreviousTermId}),
    );
  });
}

Dio _dioReturning(
  Map<String, dynamic> responseJson,
  void Function(RequestOptions options) onRequest,
) {
  return Dio()
    ..interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          onRequest(options);
          handler.resolve(
            Response<Object?>(
              requestOptions: options,
              statusCode: 200,
              data: responseJson,
            ),
          );
        },
      ),
    );
}
