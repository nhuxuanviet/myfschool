import '../../../core/network/api_client.dart';
import '../domain/grades_repository.dart';
import '../domain/semester_grades.dart';

final class ApiGradesRepository implements GradesRepository {
  const ApiGradesRepository(this._apiClient);

  static const gradesPath = '/api/v1/grades';

  final ApiClient _apiClient;

  @override
  Future<SemesterGrades> getGrades({String? termId}) async {
    final gradesJson = await _apiClient.getJson(
      gradesPath,
      queryParameters: termId == null ? null : {'termId': termId},
    );
    return SemesterGrades.fromJson(gradesJson);
  }
}
