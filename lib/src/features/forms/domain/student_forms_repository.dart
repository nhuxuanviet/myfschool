import 'student_form.dart';

abstract interface class StudentFormsRepository {
  Future<StudentFormsFeed> getForms({StudentFormStatus? status});

  Future<StudentFormDetails> getForm(String formId);

  Future<StudentFormDetails> createForm({
    required StudentFormType type,
    required String reason,
    DateTime? startsOn,
    DateTime? endsOn,
  });

  Future<StudentFormDetails> cancelForm(String formId);
}
