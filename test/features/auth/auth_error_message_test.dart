import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_exception.dart';
import 'package:myfschoolse1913/src/features/auth/application/auth_error_message.dart';

void main() {
  test('maps the backend password-reset rate-limit code', () {
    const error = ApiException(
      message: 'Too many requests',
      statusCode: 429,
      code: 'PASSWORD_RESET_RATE_LIMITED',
    );

    expect(
      authErrorMessage(error),
      'Bạn thao tác quá nhanh. Vui lòng chờ một lúc rồi thử lại.',
    );
  });
}
