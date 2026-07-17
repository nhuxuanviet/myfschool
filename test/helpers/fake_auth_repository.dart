import 'package:myfschoolse1913/src/features/auth/domain/auth_repository.dart';
import 'package:myfschoolse1913/src/features/auth/domain/auth_session.dart';
import 'package:myfschoolse1913/src/features/auth/domain/password_reset.dart';

AuthSession testAuthSession({
  String accessToken = 'access-token',
  String refreshToken = 'refresh-token',
  String? sessionId,
  String fullName = 'Nguyễn Văn A',
  AppRole role = AppRole.student,
}) {
  return AuthSession(
    sessionId: sessionId ?? refreshToken,
    tokens: AuthTokens(
      accessToken: accessToken,
      refreshToken: refreshToken,
      expiresAt: DateTime.utc(2099),
    ),
    role: role,
    // Only a student session carries a student profile.
    student: role == AppRole.student
        ? StudentSummary(
            id: 'student-id',
            studentCode: 'HS001',
            fullName: fullName,
            gradeLevel: 10,
            className: '10A1',
          )
        : null,
  );
}

class FakeAuthRepository implements AuthRepository {
  FakeAuthRepository({AuthSession? loginSession})
    : loginSession = loginSession ?? testAuthSession();

  AuthSession loginSession;
  AuthSession? restoredSession;
  AuthSession? refreshedSession;
  Object? restoreError;
  Object? loginError;
  Object? logoutError;
  Object? requestResetError;
  Object? verifyResetError;
  Object? completeResetError;
  int loginCalls = 0;
  int logoutCalls = 0;
  int clearCalls = 0;
  int requestResetCalls = 0;
  int verifyResetCalls = 0;
  int completeResetCalls = 0;
  String? lastPhoneNumber;
  String? lastPassword;
  String? lastOtp;
  String? lastNewPassword;

  @override
  Future<AuthSession?> restoreSession() async {
    if (restoreError case final error?) throw error;
    return restoredSession;
  }

  @override
  Future<AuthSession> login({
    required String phoneNumber,
    required String password,
  }) async {
    loginCalls++;
    lastPhoneNumber = phoneNumber;
    lastPassword = password;
    if (loginError case final error?) throw error;
    return loginSession;
  }

  @override
  Future<AuthSession?> refreshSession() async => refreshedSession;

  @override
  Future<void> logout() async {
    logoutCalls++;
    if (logoutError case final error?) throw error;
  }

  @override
  Future<void> clearLocalSession() async {
    clearCalls++;
  }

  @override
  Future<bool> clearLocalSessionIfCurrent(String sessionId) async {
    clearCalls++;
    return true;
  }

  @override
  Future<PasswordResetChallenge> requestPasswordReset(
    String phoneNumber,
  ) async {
    requestResetCalls++;
    lastPhoneNumber = phoneNumber;
    if (requestResetError case final error?) throw error;
    return const PasswordResetChallenge(
      challengeId: 'challenge-id',
      expiresIn: 300,
    );
  }

  @override
  Future<PasswordResetVerification> verifyPasswordReset({
    required String challengeId,
    required String otp,
  }) async {
    verifyResetCalls++;
    lastOtp = otp;
    if (verifyResetError case final error?) throw error;
    return const PasswordResetVerification(resetToken: 'reset-token');
  }

  @override
  Future<void> completePasswordReset({
    required String resetToken,
    required String newPassword,
  }) async {
    completeResetCalls++;
    lastNewPassword = newPassword;
    if (completeResetError case final error?) throw error;
  }
}
