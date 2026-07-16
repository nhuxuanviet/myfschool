import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/dio_client_factory.dart';
import '../application/auth_controller.dart';
import 'auth_data_providers.dart';
import 'authenticated_json_stream_client.dart';
import 'auth_interceptor.dart';

final authenticatedDioProvider = Provider<Dio>((ref) {
  final dio = DioClientFactory.create();
  final interceptor = AuthInterceptor(
    dio: dio,
    tokenStorage: ref.watch(authTokenStorageProvider),
    refreshSession: ref.watch(authRepositoryProvider).refreshSession,
    onSessionRefreshed: ref
        .read(authControllerProvider.notifier)
        .adoptRefreshedSession,
    onSessionExpired: ref.read(authControllerProvider.notifier).expireSession,
  );
  dio.interceptors.add(interceptor);
  ref.onDispose(() => dio.close(force: true));
  return dio;
});

final authenticatedApiClientProvider = Provider<ApiClient>(
  (ref) => ApiClient(ref.watch(authenticatedDioProvider)),
);

final authenticatedJsonStreamClientProvider =
    Provider<AuthenticatedJsonStreamClient>((ref) {
      final client = AuthenticatedJsonStreamClient(
        tokenStorage: ref.watch(authTokenStorageProvider),
        refreshSession: ref.watch(authRepositoryProvider).refreshSession,
        onSessionRefreshed: ref
            .read(authControllerProvider.notifier)
            .adoptRefreshedSession,
        onSessionExpired: ref
            .read(authControllerProvider.notifier)
            .expireSession,
      );
      ref.onDispose(client.close);
      return client;
    });
