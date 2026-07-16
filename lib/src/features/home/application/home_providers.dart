import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/home_api_repository.dart';
import '../domain/home_dashboard.dart';
import '../domain/home_repository.dart';

final homeRepositoryProvider = Provider<HomeRepository>(
  (ref) => ApiHomeRepository(ref.watch(authenticatedApiClientProvider)),
);

final homeDashboardProvider = FutureProvider.autoDispose<HomeDashboard>(
  (ref) => ref.watch(homeRepositoryProvider).getDashboard(),
);
