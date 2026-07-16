import '../../../core/network/api_client.dart';
import '../domain/health_check.dart';

abstract interface class HealthRepository {
  Future<HealthCheck> getHealth();
}

final class ApiHealthRepository implements HealthRepository {
  const ApiHealthRepository(this._apiClient);

  static const healthPath = '/api/v1/system/health';

  final ApiClient _apiClient;

  @override
  Future<HealthCheck> getHealth() async {
    final json = await _apiClient.getJson(healthPath);
    return HealthCheck.fromJson(json);
  }
}
