import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/system_health/data/health_repository.dart';
import 'package:myfschoolse1913/src/features/system_health/domain/health_check.dart';
import 'package:myfschoolse1913/src/features/system_health/providers/health_providers.dart';

class _MockApiClient extends Mock implements ApiClient {}

class _MockHealthRepository extends Mock implements HealthRepository {}

void main() {
  group('ApiHealthRepository', () {
    late ApiClient apiClient;
    late ApiHealthRepository repository;

    setUp(() {
      apiClient = _MockApiClient();
      repository = ApiHealthRepository(apiClient);
    });

    test('maps an UP public health response to a typed health check', () async {
      when(
        () => apiClient.getJson(ApiHealthRepository.healthPath),
      ).thenAnswer((_) async => {'status': 'UP'});

      final result = await repository.getHealth();

      expect(result.status, HealthStatus.up);
      verify(() => apiClient.getJson(ApiHealthRepository.healthPath)).called(1);
    });

    test('maps an unknown public health status safely', () async {
      when(
        () => apiClient.getJson(ApiHealthRepository.healthPath),
      ).thenAnswer((_) async => {'status': 'DEGRADED'});

      final result = await repository.getHealth();

      expect(result.status, HealthStatus.unknown);
    });
  });

  test('health provider delegates to the injected repository', () async {
    final repository = _MockHealthRepository();
    const expected = HealthCheck(status: HealthStatus.up);
    when(() => repository.getHealth()).thenAnswer((_) async => expected);
    final container = ProviderContainer(
      overrides: [healthRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);

    final result = await container.read(healthCheckProvider.future);

    expect(result, same(expected));
    verify(() => repository.getHealth()).called(1);
  });
}
