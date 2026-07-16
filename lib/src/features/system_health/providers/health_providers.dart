import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/network_providers.dart';
import '../data/health_repository.dart';
import '../domain/health_check.dart';

final healthRepositoryProvider = Provider<HealthRepository>(
  (ref) => ApiHealthRepository(ref.watch(apiClientProvider)),
);

final healthCheckProvider = FutureProvider<HealthCheck>(
  (ref) => ref.watch(healthRepositoryProvider).getHealth(),
);
