import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api_client.dart';
import 'dio_client_factory.dart';

final dioProvider = Provider<Dio>((ref) {
  final dio = DioClientFactory.create();
  ref.onDispose(() => dio.close(force: true));
  return dio;
});

final apiClientProvider = Provider<ApiClient>(
  (ref) => ApiClient(ref.watch(dioProvider)),
);
