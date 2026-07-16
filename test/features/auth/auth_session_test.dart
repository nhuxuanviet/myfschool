import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/auth/domain/auth_session.dart';

void main() {
  test('parses gradeLevel as a strict JSON integer', () {
    final student = StudentSummary.fromJson({
      'id': 'student-id',
      'studentCode': 'HS001',
      'fullName': 'Nguyễn Văn A',
      'gradeLevel': 10,
      'className': '10A1',
    });

    expect(student.gradeLevel, 10);
    expect(student.toJson()['gradeLevel'], isA<int>());
  });

  test('rejects a string gradeLevel from an invalid API payload', () {
    expect(
      () => StudentSummary.fromJson({
        'id': 'student-id',
        'studentCode': 'HS001',
        'fullName': 'Nguyễn Văn A',
        'gradeLevel': '10',
        'className': '10A1',
      }),
      throwsFormatException,
    );
  });
}
