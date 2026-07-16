import '../../../core/network/api_client.dart';
import '../domain/timetable.dart';
import '../domain/timetable_repository.dart';

final class ApiTimetableRepository implements TimetableRepository {
  const ApiTimetableRepository(this._apiClient);

  static const timetablePath = '/api/v1/timetable';

  final ApiClient _apiClient;

  @override
  Future<Timetable> getTimetable({DateTime? weekStart}) async {
    final timetableJson = await _apiClient.getJson(
      timetablePath,
      queryParameters: weekStart == null
          ? null
          : {'weekStart': formatTimetableDate(weekStart)},
    );
    return Timetable.fromJson(timetableJson);
  }
}
