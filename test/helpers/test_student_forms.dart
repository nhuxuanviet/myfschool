import 'package:myfschoolse1913/src/features/forms/domain/student_form.dart';

const testPendingFormId = '55555555-5555-4555-8555-555555555551';
const testApprovedFormId = '55555555-5555-4555-8555-555555555552';

Map<String, dynamic> testStudentFormSummaryJson({
  String id = testPendingFormId,
  String type = 'LEAVE_OF_ABSENCE',
  String? startsOn = '2026-07-15',
  String? endsOn = '2026-07-16',
  String status = 'SUBMITTED',
  String submittedAt = '2026-07-11T05:00:00Z',
  String updatedAt = '2026-07-11T05:00:00Z',
  bool canCancel = true,
}) {
  return {
    'id': id,
    'type': type,
    'startsOn': startsOn,
    'endsOn': endsOn,
    'status': status,
    'submittedAt': submittedAt,
    'updatedAt': updatedAt,
    'canCancel': canCancel,
  };
}

Map<String, dynamic> testStudentFormDetailsJson({
  String id = testPendingFormId,
  String type = 'LEAVE_OF_ABSENCE',
  String reason = 'Xin nghỉ học để khám sức khỏe.',
  String? startsOn = '2026-07-15',
  String? endsOn = '2026-07-16',
  String status = 'SUBMITTED',
  String submittedAt = '2026-07-11T05:00:00Z',
  String updatedAt = '2026-07-11T05:00:00Z',
  bool canCancel = true,
  List<Map<String, dynamic>>? timeline,
}) {
  return {
    ...testStudentFormSummaryJson(
      id: id,
      type: type,
      startsOn: startsOn,
      endsOn: endsOn,
      status: status,
      submittedAt: submittedAt,
      updatedAt: updatedAt,
      canCancel: canCancel,
    ),
    'reason': reason,
    'timeline':
        timeline ??
        [
          {
            'id': '66666666-6666-4666-8666-666666666661',
            'status': status,
            'occurredAt': updatedAt,
            'note': 'Đơn đã được gửi',
          },
        ],
  };
}

Map<String, dynamic> testStudentFormsFeedJson() {
  return {
    'forms': [
      testStudentFormSummaryJson(),
      testStudentFormSummaryJson(
        id: testApprovedFormId,
        type: 'STUDENT_CONFIRMATION',
        startsOn: null,
        endsOn: null,
        status: 'APPROVED',
        submittedAt: '2026-07-01T02:00:00Z',
        updatedAt: '2026-07-03T02:00:00Z',
        canCancel: false,
      ),
    ],
  };
}

StudentFormsFeed testStudentFormsFeed() {
  return StudentFormsFeed.fromJson(testStudentFormsFeedJson());
}

StudentFormDetails testPendingFormDetails() {
  return StudentFormDetails.fromJson(testStudentFormDetailsJson());
}

StudentFormDetails testApprovedFormDetails() {
  return StudentFormDetails.fromJson(
    testStudentFormDetailsJson(
      id: testApprovedFormId,
      type: 'STUDENT_CONFIRMATION',
      reason: 'Xin giấy xác nhận học sinh.',
      startsOn: null,
      endsOn: null,
      status: 'APPROVED',
      submittedAt: '2026-07-01T02:00:00Z',
      updatedAt: '2026-07-03T02:00:00Z',
      canCancel: false,
      timeline: [
        {
          'id': '66666666-6666-4666-8666-666666666662',
          'status': 'SUBMITTED',
          'occurredAt': '2026-07-01T02:00:00Z',
          'note': 'Đơn đã được gửi',
        },
        {
          'id': '66666666-6666-4666-8666-666666666663',
          'status': 'APPROVED',
          'occurredAt': '2026-07-03T02:00:00Z',
          'note': 'Đơn đã được phê duyệt',
        },
      ],
    ),
  );
}
