import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/student_forms_api_repository.dart';
import '../domain/student_form.dart';
import '../domain/student_forms_repository.dart';

final studentFormsRepositoryProvider = Provider<StudentFormsRepository>(
  (ref) => ApiStudentFormsRepository(ref.watch(authenticatedApiClientProvider)),
);

final studentFormsStatusFilterProvider =
    NotifierProvider.autoDispose<_StudentFormsStatusFilter, StudentFormStatus?>(
      _StudentFormsStatusFilter.new,
    );

final class _StudentFormsStatusFilter extends Notifier<StudentFormStatus?> {
  @override
  StudentFormStatus? build() => null;

  void select(StudentFormStatus? status) => state = status;
}

final studentFormsFeedProvider = FutureProvider.autoDispose<StudentFormsFeed>((
  ref,
) {
  return ref
      .watch(studentFormsRepositoryProvider)
      .getForms(status: ref.watch(studentFormsStatusFilterProvider));
});

final studentFormDetailsProvider = FutureProvider.autoDispose
    .family<StudentFormDetails, String>((ref, formId) async {
      final canonical = canonicalStudentFormId(formId);
      if (canonical == null) {
        throw const FormatException(
          'The requested student form ID is invalid.',
        );
      }
      final details = await ref
          .watch(studentFormsRepositoryProvider)
          .getForm(canonical);
      if (details.summary.id != canonical) {
        throw const FormatException(
          'The student form response did not match the requested form.',
        );
      }
      return details;
    });

final class StudentFormMutationState {
  const StudentFormMutationState({
    this.isSubmitting = false,
    this.activeFormId,
    this.errorMessage,
  });

  final bool isSubmitting;
  final String? activeFormId;
  final String? errorMessage;
}

final studentFormMutationProvider =
    NotifierProvider.autoDispose<
      StudentFormMutationController,
      StudentFormMutationState
    >(StudentFormMutationController.new);

class StudentFormMutationController extends Notifier<StudentFormMutationState> {
  @override
  StudentFormMutationState build() => const StudentFormMutationState();

  Future<StudentFormDetails?> create({
    required StudentFormType type,
    required String reason,
    DateTime? startsOn,
    DateTime? endsOn,
  }) async {
    if (state.isSubmitting) return null;
    state = const StudentFormMutationState(isSubmitting: true);
    try {
      final details = await ref
          .read(studentFormsRepositoryProvider)
          .createForm(
            type: type,
            reason: reason,
            startsOn: startsOn,
            endsOn: endsOn,
          );
      if (!ref.mounted) return null;
      state = const StudentFormMutationState();
      ref.invalidate(studentFormsFeedProvider);
      return details;
    } on Object {
      if (!ref.mounted) return null;
      state = const StudentFormMutationState(
        errorMessage:
            'Không thể gửi đơn. Vui lòng kiểm tra thông tin và thử lại.',
      );
      return null;
    }
  }

  Future<bool> cancel(String formId) async {
    if (state.isSubmitting) return false;
    final canonical = canonicalStudentFormId(formId);
    if (canonical == null) return false;
    state = StudentFormMutationState(
      isSubmitting: true,
      activeFormId: canonical,
    );
    try {
      final details = await ref
          .read(studentFormsRepositoryProvider)
          .cancelForm(canonical);
      if (!ref.mounted) return false;
      if (details.summary.id != canonical) {
        throw const FormatException(
          'The cancelled form response did not match the requested form.',
        );
      }
      state = const StudentFormMutationState();
      ref.invalidate(studentFormDetailsProvider(canonical));
      ref.invalidate(studentFormsFeedProvider);
      return true;
    } on Object {
      if (!ref.mounted) return false;
      state = StudentFormMutationState(
        activeFormId: canonical,
        errorMessage: 'Không thể hủy đơn. Trạng thái có thể đã thay đổi.',
      );
      ref.invalidate(studentFormDetailsProvider(canonical));
      ref.invalidate(studentFormsFeedProvider);
      return false;
    }
  }
}
