import 'package:dio/dio.dart';

import '../config/app_environment.dart';

abstract final class DioClientFactory {
  static const _timeout = Duration(seconds: 15);

  static Dio create({String? baseUrl}) {
    return Dio(
      BaseOptions(
        baseUrl: baseUrl ?? AppEnvironment.apiBaseUrl,
        connectTimeout: _timeout,
        sendTimeout: _timeout,
        receiveTimeout: _timeout,
        headers: const {Headers.acceptHeader: Headers.jsonContentType},
      ),
    );
  }
}
