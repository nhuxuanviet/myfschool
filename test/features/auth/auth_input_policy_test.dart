import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/auth/domain/auth_input_policy.dart';

void main() {
  test('accepts supported Vietnamese mobile formats only', () {
    expect(AuthInputPolicy.isVietnameseMobile('0912345678'), isTrue);
    expect(AuthInputPolicy.isVietnameseMobile('84912345678'), isTrue);
    expect(AuthInputPolicy.isVietnameseMobile('0212345678'), isFalse);
    expect(AuthInputPolicy.isVietnameseMobile('912345678'), isFalse);
  });

  test('accepts only passwords compatible with the backend policy', () {
    expect(AuthInputPolicy.isStrongPassword('Student@123'), isTrue);
    expect(AuthInputPolicy.isStrongPassword('Abcdef1é'), isFalse);
    expect(AuthInputPolicy.isStrongPassword('NoSpecial123'), isFalse);
    expect(AuthInputPolicy.isStrongPassword('no-uppercase@123'), isFalse);
    final oversizedUnicode = 'A1@${List.filled(30, '界').join()}';
    expect(AuthInputPolicy.isStrongPassword(oversizedUnicode), isFalse);
  });
}
