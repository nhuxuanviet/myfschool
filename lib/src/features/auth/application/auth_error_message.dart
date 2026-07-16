import '../../../core/network/api_exception.dart';

String authErrorMessage(Object error) {
  if (error is! ApiException) {
    return 'Đã xảy ra lỗi. Vui lòng thử lại.';
  }

  return switch (error.code) {
    'INVALID_CREDENTIALS' => 'Số điện thoại hoặc mật khẩu không đúng.',
    'ACCOUNT_LOCKED' => 'Tài khoản đang bị khóa. Vui lòng liên hệ nhà trường.',
    'ACCOUNT_DISABLED' => 'Tài khoản hiện không hoạt động.',
    'INVALID_OTP' || 'OTP_INVALID' => 'Mã OTP không đúng hoặc đã hết hạn.',
    'OTP_EXPIRED' => 'Mã OTP đã hết hạn. Vui lòng gửi mã mới.',
    'OTP_ATTEMPTS_EXCEEDED' =>
      'Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi mã mới.',
    'RESET_TOKEN_INVALID' || 'RESET_TOKEN_EXPIRED' =>
      'Phiên đặt lại mật khẩu đã hết hạn. Vui lòng bắt đầu lại.',
    'RESET_CHALLENGE_INVALID' =>
      'Yêu cầu đặt lại mật khẩu đã hết hạn. Vui lòng bắt đầu lại.',
    'WEAK_PASSWORD' =>
      'Mật khẩu cần có chữ hoa, chữ thường, số và ký tự đặc biệt.',
    'PASSWORD_RESET_RATE_LIMITED' || 'TOO_MANY_REQUESTS' =>
      'Bạn thao tác quá nhanh. Vui lòng chờ một lúc rồi thử lại.',
    'VALIDATION_FAILED' when error.fieldErrors.isNotEmpty =>
      error.fieldErrors.first.message,
    _ when error.statusCode == 429 =>
      'Bạn thao tác quá nhanh. Vui lòng chờ một lúc rồi thử lại.',
    _ when error.statusCode == null =>
      'Không thể kết nối đến hệ thống. Vui lòng kiểm tra mạng.',
    _ => error.message,
  };
}
