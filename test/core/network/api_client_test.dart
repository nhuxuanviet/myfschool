import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/core/network/api_exception.dart';

void main() {
  test('returns a JSON object response', () async {
    final dio = _stubbedDio(statusCode: 200, data: {'status': 'UP'});

    final result = await ApiClient(dio).getJson('/health');

    expect(result, {'status': 'UP'});
  });

  test('returns a JSON object from a DELETE response', () async {
    final dio = _stubbedDio(
      statusCode: 200,
      data: {'registration': 'CANCELLED'},
    );

    final result = await ApiClient(
      dio,
    ).deleteJson('/api/v1/events/event/registrations');

    expect(result, {'registration': 'CANCELLED'});
  });

  test('preserves ProblemDetail metadata and validation errors', () async {
    final dio = _stubbedDio(
      statusCode: 400,
      data: {
        'detail': 'Request validation failed',
        'code': 'VALIDATION_FAILED',
        'instance': '/api/v1/auth/login',
        'errors': [
          {'field': 'phone', 'message': 'must not be blank'},
        ],
      },
    );

    await expectLater(
      ApiClient(dio).postJson('/auth/login', data: const {}),
      throwsA(
        isA<ApiException>()
            .having((error) => error.statusCode, 'statusCode', 400)
            .having((error) => error.code, 'code', 'VALIDATION_FAILED')
            .having((error) => error.instance, 'instance', '/api/v1/auth/login')
            .having(
              (error) => error.fieldErrors.single.field,
              'field error',
              'phone',
            ),
      ),
    );
  });
}

Dio _stubbedDio({required int statusCode, required Object data}) {
  final dio = Dio(BaseOptions(validateStatus: (status) => status! < 400));
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        final response = Response<Object?>(
          requestOptions: options,
          statusCode: statusCode,
          data: data,
        );
        if (statusCode >= 400) {
          handler.reject(
            DioException.badResponse(
              statusCode: statusCode,
              requestOptions: options,
              response: response,
            ),
          );
          return;
        }
        handler.resolve(response);
      },
    ),
  );
  return dio;
}
