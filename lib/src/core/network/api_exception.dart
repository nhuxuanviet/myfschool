import 'package:dio/dio.dart';

class ApiFieldError {
  const ApiFieldError({required this.field, required this.message});

  factory ApiFieldError.fromJson(Map<String, dynamic> json) {
    return ApiFieldError(
      field: json['field']?.toString() ?? '',
      message: json['message']?.toString() ?? '',
    );
  }

  final String field;
  final String message;
}

class ApiException implements Exception {
  const ApiException({
    required this.message,
    this.statusCode,
    this.code,
    this.instance,
    this.fieldErrors = const [],
    this.cause,
  });

  factory ApiException.fromDio(DioException error) {
    final body = error.response?.data;
    final problem = body is Map
        ? Map<String, dynamic>.from(body)
        : const <String, dynamic>{};
    final rawErrors = problem['errors'];
    final fieldErrors = rawErrors is List
        ? rawErrors
              .whereType<Map>()
              .map((item) => ApiFieldError.fromJson(Map.from(item)))
              .toList(growable: false)
        : const <ApiFieldError>[];

    return ApiException(
      message: _problemMessage(problem) ?? _transportMessage(error),
      statusCode: error.response?.statusCode,
      code: problem['code']?.toString(),
      instance: problem['instance']?.toString(),
      fieldErrors: fieldErrors,
      cause: error,
    );
  }

  final String message;
  final int? statusCode;
  final String? code;
  final String? instance;
  final List<ApiFieldError> fieldErrors;
  final Object? cause;

  static String? _problemMessage(Map<String, dynamic> problem) {
    for (final key in const ['detail', 'message', 'title']) {
      final value = problem[key]?.toString().trim();
      if (value != null && value.isNotEmpty) return value;
    }
    return null;
  }

  static String _transportMessage(DioException error) {
    return switch (error.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout ||
      DioExceptionType.receiveTimeout => 'The request timed out.',
      DioExceptionType.connectionError => 'Unable to connect to the server.',
      _ => 'The request could not be completed.',
    };
  }

  @override
  String toString() {
    final status = statusCode == null ? '' : ' ($statusCode)';
    final errorCode = code == null ? '' : ' [$code]';
    return 'ApiException$status$errorCode: $message';
  }
}
