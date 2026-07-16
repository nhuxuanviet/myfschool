import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/network/network_providers.dart';
import '../domain/auth_repository.dart';
import 'auth_api_repository.dart';
import 'auth_token_storage.dart';

final flutterSecureStorageProvider = Provider<FlutterSecureStorage>(
  (ref) => const FlutterSecureStorage(),
);

final authTokenStorageProvider = Provider<AuthTokenStorage>(
  (ref) => SecureAuthTokenStorage(ref.watch(flutterSecureStorageProvider)),
);

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthApiRepository(
    ref.watch(apiClientProvider),
    ref.watch(authTokenStorageProvider),
  ),
);
