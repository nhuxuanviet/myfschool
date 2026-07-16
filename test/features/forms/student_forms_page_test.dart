import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/forms/application/student_forms_providers.dart';
import 'package:myfschoolse1913/src/features/forms/domain/student_form.dart';
import 'package:myfschoolse1913/src/features/forms/domain/student_forms_repository.dart';
import 'package:myfschoolse1913/src/features/forms/presentation/student_form_create_page.dart';
import 'package:myfschoolse1913/src/features/forms/presentation/student_form_details_page.dart';
import 'package:myfschoolse1913/src/features/forms/presentation/student_forms_page.dart';

import '../../helpers/test_student_forms.dart';

const _createdFormId = '77777777-7777-4777-8777-777777777777';

void main() {
  testWidgets('lists student forms through accessible controls', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeStudentFormsRepository();
    final router = await _pumpForms(tester, repository);
    addTearDown(router.dispose);

    expect(find.byType(StudentFormsPage), findsOneWidget);
    expect(find.bySemanticsLabel('Đơn từ'), findsOneWidget);
    expect(find.bySemanticsLabel('Tạo đơn mới'), findsWidgets);
    expect(repository.requestedStatuses, [null]);
    expect(
      find.bySemanticsLabel('Đơn Giấy xác nhận học sinh. Trạng thái Đã duyệt.'),
      findsOneWidget,
    );
    semantics.dispose();
  });

  testWidgets('opens an owned form and renders its status timeline', (
    tester,
  ) async {
    final repository = _FakeStudentFormsRepository();
    final router = await _pumpForms(tester, repository);
    addTearDown(router.dispose);

    await tester.tap(find.bySemanticsLabel('Xem đơn $testPendingFormId'));
    await tester.pumpAndSettle();

    expect(router.state.uri.path, '/forms/$testPendingFormId');
    expect(find.byType(StudentFormDetailsPage), findsOneWidget);
    expect(
      find.bySemanticsLabel('Chi tiết đơn $testPendingFormId'),
      findsOneWidget,
    );
    expect(find.text('Tiến trình xử lý'), findsOneWidget);
  });

  testWidgets('creates a confirmation form and opens its details', (
    tester,
  ) async {
    final repository = _FakeStudentFormsRepository();
    final router = await _pumpForms(
      tester,
      repository,
      initialLocation: AppRoutes.formCreate,
    );
    addTearDown(router.dispose);

    expect(find.byType(StudentFormCreatePage), findsOneWidget);
    await tester.tap(find.bySemanticsLabel('Loại đơn'));
    await tester.pumpAndSettle();
    await tester.tap(
      find.bySemanticsLabel('Chọn loại đơn Giấy xác nhận học sinh'),
    );
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byType(TextField).first,
      'Xin giấy xác nhận để bổ sung hồ sơ.',
    );
    await tester.tap(find.bySemanticsLabel('Gửi đơn'));
    await tester.pumpAndSettle();

    expect(repository.createdTypes, [StudentFormType.studentConfirmation]);
    expect(router.state.uri.path, '/forms/$_createdFormId');
    expect(
      find.bySemanticsLabel('Chi tiết đơn $_createdFormId'),
      findsOneWidget,
    );
  });

  testWidgets('cancels a pending form after confirmation', (tester) async {
    final repository = _FakeStudentFormsRepository();
    final router = await _pumpForms(
      tester,
      repository,
      initialLocation: '/forms/$testPendingFormId',
    );
    addTearDown(router.dispose);

    final detailsScroll = find
        .descendant(
          of: find.byType(StudentFormDetailsPage),
          matching: find.byType(Scrollable),
        )
        .first;
    await tester.drag(detailsScroll, const Offset(0, -400));
    await tester.pumpAndSettle();
    await tester.tap(find.bySemanticsLabel('Hủy đơn'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xác nhận hủy đơn'));
    await tester.pumpAndSettle();

    expect(repository.cancelledIds, [testPendingFormId]);
    expect(find.text('Trạng thái: Đã hủy'), findsOneWidget);
    expect(find.bySemanticsLabel('Hủy đơn'), findsNothing);
  });

  testWidgets('canonicalizes uppercase IDs and rejects malformed deep links', (
    tester,
  ) async {
    final repository = _FakeStudentFormsRepository();
    final router = await _pumpForms(
      tester,
      repository,
      initialLocation: '/forms/${testPendingFormId.toUpperCase()}',
    );
    addTearDown(router.dispose);

    expect(router.state.uri.path, '/forms/$testPendingFormId');
    expect(repository.requestedIds, [testPendingFormId]);

    router.go('/forms/not-a-form-id');
    await tester.pumpAndSettle();
    expect(router.state.uri.path, AppRoutes.forms);
    expect(find.byType(StudentFormsPage), findsOneWidget);
  });
}

Future<GoRouter> _pumpForms(
  WidgetTester tester,
  StudentFormsRepository repository, {
  String initialLocation = AppRoutes.forms,
}) async {
  final router = createAppRouter(initialLocation: initialLocation);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [studentFormsRepositoryProvider.overrideWithValue(repository)],
      child: FptSchoolsApp(router: router),
    ),
  );
  await tester.pumpAndSettle();
  return router;
}

final class _FakeStudentFormsRepository implements StudentFormsRepository {
  _FakeStudentFormsRepository()
    : _details = {
        testPendingFormId: testPendingFormDetails(),
        testApprovedFormId: testApprovedFormDetails(),
      };

  final Map<String, StudentFormDetails> _details;
  final List<StudentFormStatus?> requestedStatuses = [];
  final List<String> requestedIds = [];
  final List<StudentFormType> createdTypes = [];
  final List<String> cancelledIds = [];

  @override
  Future<StudentFormsFeed> getForms({StudentFormStatus? status}) {
    requestedStatuses.add(status);
    final forms = _details.values
        .map((details) => details.summary)
        .where((form) => status == null || form.status == status)
        .toList(growable: false);
    return Future.value(StudentFormsFeed(forms: forms));
  }

  @override
  Future<StudentFormDetails> getForm(String formId) {
    requestedIds.add(formId);
    final details = _details[formId];
    if (details == null) {
      return Future.error(StateError('Missing form $formId'));
    }
    return Future.value(details);
  }

  @override
  Future<StudentFormDetails> createForm({
    required StudentFormType type,
    required String reason,
    DateTime? startsOn,
    DateTime? endsOn,
  }) {
    createdTypes.add(type);
    final details = StudentFormDetails.fromJson(
      testStudentFormDetailsJson(
        id: _createdFormId,
        type: type.apiValue,
        reason: reason,
        startsOn: startsOn == null ? null : formatIsoStudentFormDate(startsOn),
        endsOn: endsOn == null ? null : formatIsoStudentFormDate(endsOn),
      ),
    );
    _details[details.summary.id] = details;
    return Future.value(details);
  }

  @override
  Future<StudentFormDetails> cancelForm(String formId) {
    cancelledIds.add(formId);
    final current = _details[formId]!;
    final details = StudentFormDetails.fromJson(
      testStudentFormDetailsJson(
        id: formId,
        type: current.summary.type.apiValue,
        reason: current.reason,
        startsOn: current.summary.startsOn == null
            ? null
            : formatIsoStudentFormDate(current.summary.startsOn!),
        endsOn: current.summary.endsOn == null
            ? null
            : formatIsoStudentFormDate(current.summary.endsOn!),
        status: 'CANCELLED',
        submittedAt: '2026-07-11T05:00:00Z',
        updatedAt: '2026-07-11T06:00:00Z',
        canCancel: false,
        timeline: [
          {
            'id': '66666666-6666-4666-8666-666666666661',
            'status': 'SUBMITTED',
            'occurredAt': '2026-07-11T05:00:00Z',
            'note': 'Đơn đã được gửi',
          },
          {
            'id': '66666666-6666-4666-8666-666666666665',
            'status': 'CANCELLED',
            'occurredAt': '2026-07-11T06:00:00Z',
            'note': 'Học sinh đã hủy đơn',
          },
        ],
      ),
    );
    _details[formId] = details;
    return Future.value(details);
  }
}
