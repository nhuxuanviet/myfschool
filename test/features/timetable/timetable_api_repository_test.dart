import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/timetable/data/timetable_api_repository.dart';

import '../../helpers/test_timetable.dart';

void main() {
  test(
    'loads the current timetable without a weekStart query parameter',
    () async {
      late RequestOptions requestedOptions;
      final dio = _dioReturning(testTimetableJson(), (options) {
        requestedOptions = options;
      });

      final timetable = await ApiTimetableRepository(
        ApiClient(dio),
      ).getTimetable();

      expect(requestedOptions.path, ApiTimetableRepository.timetablePath);
      expect(requestedOptions.queryParameters, isEmpty);
      expect(timetable.days, hasLength(7));
    },
  );

  test('formats an explicit weekStart as an ISO school date', () async {
    late RequestOptions requestedOptions;
    final dio = _dioReturning(testTimetableJson(weekStart: '2026-07-13'), (
      options,
    ) {
      requestedOptions = options;
    });

    await ApiTimetableRepository(
      ApiClient(dio),
    ).getTimetable(weekStart: DateTime.utc(2026, 7, 13));

    expect(
      requestedOptions.queryParameters,
      equals(const {'weekStart': '2026-07-13'}),
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
