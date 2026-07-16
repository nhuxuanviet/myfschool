import 'auth_session.dart';
import 'password_reset.dart';

abstract interface class AuthRepository {
  Future<AuthSession> login({
    required String phoneNumber,
    required String password,
  });

  Future<AuthSession?> restoreSession();

  Future<AuthSession?> refreshSession();

  Future<void> logout();

  Future<void> clearLocalSession();

  Future<bool> clearLocalSessionIfCurrent(String sessionId);

  Future<PasswordResetChallenge> requestPasswordReset(String phoneNumber);

  Future<PasswordResetVerification> verifyPasswordReset({
    required String challengeId,
    required String otp,
  });

  Future<void> completePasswordReset({
    required String resetToken,
    required String newPassword,
  });
}
