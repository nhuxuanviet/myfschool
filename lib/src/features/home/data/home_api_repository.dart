import '../../../core/network/api_client.dart';
import '../domain/home_dashboard.dart';
import '../domain/home_repository.dart';

final class ApiHomeRepository implements HomeRepository {
  const ApiHomeRepository(this._apiClient);

  static const dashboardPath = '/api/v1/home';

  final ApiClient _apiClient;

  @override
  Future<HomeDashboard> getDashboard() async {
    final json = await _apiClient.getJson(dashboardPath);
    return HomeDashboard.fromJson(json);
  }
}
