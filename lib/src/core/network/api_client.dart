import 'package:dio/dio.dart';

import 'api_exception.dart';

class ApiClient {
  const ApiClient(this._dio);

  final Dio _dio;

  Future<Map<String, dynamic>> getJson(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) {
    return _requestJson(
      () => _dio.get<Object?>(path, queryParameters: queryParameters),
    );
  }

  Future<Map<String, dynamic>> postJson(String path, {Object? data}) {
    return _requestJson(() => _dio.post<Object?>(path, data: data));
  }

  Future<void> post(String path, {Object? data}) async {
    try {
      await _dio.post<Object?>(path, data: data);
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    }
  }

  Future<Map<String, dynamic>> putJson(String path, {Object? data}) {
    return _requestJson(() => _dio.put<Object?>(path, data: data));
  }

  Future<Map<String, dynamic>> patchJson(String path, {Object? data}) {
    return _requestJson(() => _dio.patch<Object?>(path, data: data));
  }

  Future<void> delete(String path, {Object? data}) async {
    try {
      await _dio.delete<Object?>(path, data: data);
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    }
  }

  Future<Map<String, dynamic>> deleteJson(String path, {Object? data}) {
    return _requestJson(() => _dio.delete<Object?>(path, data: data));
  }

  Future<Map<String, dynamic>> _requestJson(
    Future<Response<Object?>> Function() request,
  ) async {
    try {
      final response = await request();
      final data = response.data;
      if (data is! Map) {
        throw ApiException(
          message: 'The server returned an invalid JSON object.',
          statusCode: response.statusCode,
        );
      }

      return Map<String, dynamic>.from(data);
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    }
  }
}
