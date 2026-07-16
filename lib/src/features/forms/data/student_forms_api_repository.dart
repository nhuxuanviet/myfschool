import '../../../core/network/api_client.dart';
import '../domain/student_form.dart';
import '../domain/student_forms_repository.dart';

final class ApiStudentFormsRepository implements StudentFormsRepository {
  const ApiStudentFormsRepository(this._apiClient);

  static const formsPath = '/api/v1/forms';

  final ApiClient _apiClient;

  @override
  Future<StudentFormsFeed> getForms({StudentFormStatus? status}) async {
    final json = await _apiClient.getJson(
      formsPath,
      queryParameters: status == null ? null : {'status': status.apiValue},
    );
    return StudentFormsFeed.fromJson(json);
  }

  @override
  Future<StudentFormDetails> getForm(String formId) async {
    return StudentFormDetails.fromJson(
      await _apiClient.getJson(_formPath(formId)),
    );
  }

  @override
  Future<StudentFormDetails> createForm({
    required StudentFormType type,
    required String reason,
    DateTime? startsOn,
    DateTime? endsOn,
  }) async {
    return StudentFormDetails.fromJson(
      await _apiClient.postJson(
        formsPath,
        data: {
          'type': type.apiValue,
          'reason': reason,
          'startsOn': startsOn == null
              ? null
              : formatIsoStudentFormDate(startsOn),
          'endsOn': endsOn == null ? null : formatIsoStudentFormDate(endsOn),
        },
      ),
    );
  }

  @override
  Future<StudentFormDetails> cancelForm(String formId) async {
    return StudentFormDetails.fromJson(
      await _apiClient.deleteJson(_formPath(formId)),
    );
  }

  static String _formPath(String formId) {
    final canonical = canonicalStudentFormId(formId);
    if (canonical == null) {
      throw ArgumentError.value(formId, 'formId', 'must be a UUID');
    }
    return '$formsPath/${Uri.encodeComponent(canonical)}';
  }
}
