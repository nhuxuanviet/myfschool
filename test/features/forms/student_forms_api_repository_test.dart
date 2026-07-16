import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/forms/data/student_forms_api_repository.dart';
import 'package:myfschoolse1913/src/features/forms/domain/student_form.dart';

import '../../helpers/test_student_forms.dart';

void main() {
  test('loads forms with an optional status filter', () async {
    late RequestOptions request;
    final repository = ApiStudentFormsRepository(
      ApiClient(
        _dioReturning((options) {
          request = options;
          return testStudentFormsFeedJson();
        }),
      ),
    );

    final feed = await repository.getForms(status: StudentFormStatus.approved);

    expect(feed.forms, hasLength(2));
    expect(request.path, ApiStudentFormsRepository.formsPath);
    expect(request.queryParameters, {'status': 'APPROVED'});
  });

  test(
    'uses detail, create, and cancel endpoints with exact payloads',
    () async {
      final requests = <RequestOptions>[];
      var responseIndex = 0;
      final responses = [
        testStudentFormDetailsJson(),
        testStudentFormDetailsJson(
          type: 'STUDENT_CONFIRMATION',
          startsOn: null,
          endsOn: null,
        ),
        testStudentFormDetailsJson(
          status: 'CANCELLED',
          updatedAt: '2026-07-11T06:00:00Z',
          canCancel: false,
          timeline: [
            {
              'id': '66666666-6666-4666-8666-666666666661',
              'status': 'SUBMITTED',
              'occurredAt': '2026-07-11T05:00:00Z',
              'note': 'Đơn đã được gửi',
            },
            {
              'id': '66666666-6666-4666-8666-666666666665',
              'status': 'CANCELLED',
              'occurredAt': '2026-07-11T06:00:00Z',
              'note': 'Học sinh đã hủy đơn',
            },
          ],
        ),
      ];
      final repository = ApiStudentFormsRepository(
        ApiClient(
          _dioReturning((options) {
            requests.add(options);
            return responses[responseIndex++];
          }),
        ),
      );

      await repository.getForm(testPendingFormId);
      await repository.createForm(
        type: StudentFormType.studentConfirmation,
        reason: 'Xin giấy xác nhận.',
      );
      await repository.cancelForm(testPendingFormId);

      expect(requests.map((request) => request.method), [
        'GET',
        'POST',
        'DELETE',
      ]);
      expect(requests.map((request) => request.path), [
        '/api/v1/forms/$testPendingFormId',
        '/api/v1/forms',
        '/api/v1/forms/$testPendingFormId',
      ]);
      expect(requests[1].data, {
        'type': 'STUDENT_CONFIRMATION',
        'reason': 'Xin giấy xác nhận.',
        'startsOn': null,
        'endsOn': null,
      });
    },
  );
}

Dio _dioReturning(
  Map<String, dynamic> Function(RequestOptions options) responseFor,
) {
  return Dio()
    ..interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          handler.resolve(
            Response<Object?>(
              requestOptions: options,
              statusCode: 200,
              data: responseFor(options),
            ),
          );
        },
      ),
    );
}
