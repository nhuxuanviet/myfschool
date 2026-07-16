import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/forms/domain/student_form.dart';

import '../../helpers/test_student_forms.dart';

void main() {
  test('parses list and details contracts with a chronological timeline', () {
    final feed = StudentFormsFeed.fromJson(testStudentFormsFeedJson());
    final details = testApprovedFormDetails();

    expect(feed.forms, hasLength(2));
    expect(feed.forms.first.type, StudentFormType.leaveOfAbsence);
    expect(feed.forms.first.canCancel, isTrue);
    expect(details.summary.status, StudentFormStatus.approved);
    expect(details.timeline.map((entry) => entry.status), [
      StudentFormStatus.submitted,
      StudentFormStatus.approved,
    ]);
  });

  test('uses Vietnam time for submitted and timeline instants', () {
    expect(
      formatStudentFormInstant(DateTime.parse('2026-07-11T18:30:00Z')),
      '01:30 • 12/07/2026',
    );
  });

  test('rejects invalid date rules, actions, and timelines', () {
    final invalidLeave = testStudentFormSummaryJson(endsOn: '2026-07-14');
    final datesOnConfirmation = testStudentFormSummaryJson(
      type: 'STUDENT_CONFIRMATION',
    );
    final invalidAction = testStudentFormSummaryJson(canCancel: false);
    final wrongTimeline = testStudentFormDetailsJson(
      timeline: [
        {
          'id': '66666666-6666-4666-8666-666666666669',
          'status': 'APPROVED',
          'occurredAt': '2026-07-11T05:00:00Z',
          'note': null,
        },
      ],
    );

    expect(
      () => StudentFormSummary.fromJson(invalidLeave),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => StudentFormSummary.fromJson(datesOnConfirmation),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => StudentFormSummary.fromJson(invalidAction),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => StudentFormDetails.fromJson(wrongTimeline),
      throwsA(isA<FormatException>()),
    );
  });

  test('validates ISO dates and canonicalizes deep-link IDs', () {
    expect(tryParseStudentFormDate('2026-02-29'), isNull);
    expect(tryParseStudentFormDate('2026-07-15'), DateTime.utc(2026, 7, 15));
    expect(
      canonicalStudentFormId(testPendingFormId.toUpperCase()),
      testPendingFormId,
    );
  });
}
