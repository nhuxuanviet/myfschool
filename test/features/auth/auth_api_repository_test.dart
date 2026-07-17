import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/core/network/api_exception.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_api_repository.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_token_storage.dart';
import 'package:myfschoolse1913/src/features/auth/domain/auth_session.dart';

import '../../helpers/fake_auth_repository.dart';

class _MockApiClient extends Mock implements ApiClient {}

class _MockTokenStorage extends Mock implements AuthTokenStorage {}

final class _MemoryTokenStorage implements AuthTokenStorage {
  _MemoryTokenStorage(this.session);

  AuthSession? session;

  @override
  Future<AuthSession?> readSession() async => session;

  @override
  Future<void> writeSession(AuthSession value) async {
    session = value;
  }

  @override
  Future<void> clear() async {
    session = null;
  }
}

void main() {
  late ApiClient apiClient;
  late AuthTokenStorage tokenStorage;
  late AuthApiRepository repository;
  final now = DateTime.utc(2026, 7, 10, 12);

  setUpAll(() {
    registerFallbackValue(testAuthSession());
  });

  setUp(() {
    apiClient = _MockApiClient();
    tokenStorage = _MockTokenStorage();
    repository = AuthApiRepository(apiClient, tokenStorage, now: () => now);
  });

  test(
    'login sends the locked DTO and persists the returned session',
    () async {
      when(
        () => apiClient.postJson(
          AuthApiRepository.loginPath,
          data: any(named: 'data'),
        ),
      ).thenAnswer((_) async => _loginResponse);
      when(() => tokenStorage.writeSession(any())).thenAnswer((_) async {});

      final session = await repository.login(
        phoneNumber: '0912345678',
        password: 'secret-password',
      );

      expect(session.requireStudent.fullName, 'Nguyễn Văn A');
      expect(session.tokens.expiresAt, now.add(const Duration(hours: 1)));
      final request = verify(
        () => apiClient.postJson(
          AuthApiRepository.loginPath,
          data: captureAny(named: 'data'),
        ),
      ).captured.single;
      expect(request, {
        'phoneNumber': '0912345678',
        'password': 'secret-password',
      });
      verify(() => tokenStorage.writeSession(session)).called(1);
    },
  );

  test('restores an unexpired local session without a request', () async {
    final session = testAuthSession();
    when(() => tokenStorage.readSession()).thenAnswer((_) async => session);

    final restored = await repository.restoreSession();

    expect(restored, same(session));
    verifyNever(
      () => apiClient.postJson(
        AuthApiRepository.refreshPath,
        data: any(named: 'data'),
      ),
    );
  });

  test('clears an invalid refresh session', () async {
    final session = testAuthSession();
    when(() => tokenStorage.readSession()).thenAnswer((_) async => session);
    when(
      () => apiClient.postJson(
        AuthApiRepository.refreshPath,
        data: any(named: 'data'),
      ),
    ).thenThrow(
      const ApiException(
        message: 'Invalid refresh token',
        statusCode: 401,
        code: 'INVALID_REFRESH_TOKEN',
      ),
    );
    when(() => tokenStorage.clear()).thenAnswer((_) async {});

    final result = await repository.refreshSession();

    expect(result, isNull);
    verify(() => tokenStorage.clear()).called(1);
  });

  test(
    'does not restore a refresh response that finishes after logout',
    () async {
      final oldSession = testAuthSession(
        accessToken: 'old-access',
        refreshToken: 'old-refresh',
      );
      final memoryStorage = _MemoryTokenStorage(oldSession);
      final refreshStarted = Completer<void>();
      final refreshResponse = Completer<Map<String, dynamic>>();
      repository = AuthApiRepository(apiClient, memoryStorage, now: () => now);
      when(
        () => apiClient.postJson(
          AuthApiRepository.refreshPath,
          data: any(named: 'data'),
        ),
      ).thenAnswer((_) {
        refreshStarted.complete();
        return refreshResponse.future;
      });
      when(
        () => apiClient.post(
          AuthApiRepository.logoutPath,
          data: any(named: 'data'),
        ),
      ).thenAnswer((_) async {});

      final pendingRefresh = repository.refreshSession();
      await refreshStarted.future;
      await repository.logout();
      refreshResponse.complete(_loginResponse);

      expect(await pendingRefresh, isNull);
      expect(memoryStorage.session, isNull);
      verify(
        () => apiClient.post(
          AuthApiRepository.logoutPath,
          data: {'refreshToken': 'old-refresh'},
        ),
      ).called(1);
    },
  );

  test('stale refresh resolves to a newer login session', () async {
    final memoryStorage = _MemoryTokenStorage(
      testAuthSession(accessToken: 'old-access', refreshToken: 'old-refresh'),
    );
    final refreshStarted = Completer<void>();
    final refreshResponse = Completer<Map<String, dynamic>>();
    repository = AuthApiRepository(apiClient, memoryStorage, now: () => now);
    when(
      () => apiClient.postJson(
        AuthApiRepository.refreshPath,
        data: any(named: 'data'),
      ),
    ).thenAnswer((_) {
      refreshStarted.complete();
      return refreshResponse.future;
    });
    when(
      () => apiClient.post(
        AuthApiRepository.logoutPath,
        data: any(named: 'data'),
      ),
    ).thenAnswer((_) async {});
    when(
      () => apiClient.postJson(
        AuthApiRepository.loginPath,
        data: any(named: 'data'),
      ),
    ).thenAnswer((_) async => _newLoginResponse);

    final pendingRefresh = repository.refreshSession();
    await refreshStarted.future;
    await repository.logout();
    final newSession = await repository.login(
      phoneNumber: '0912345678',
      password: 'NewStudent@123',
    );
    refreshResponse.complete(_loginResponse);

    expect(await pendingRefresh, same(newSession));
    expect(memoryStorage.session, same(newSession));
  });

  test('shares one refresh operation across concurrent callers', () async {
    final memoryStorage = _MemoryTokenStorage(testAuthSession());
    repository = AuthApiRepository(apiClient, memoryStorage, now: () => now);
    when(
      () => apiClient.postJson(
        AuthApiRepository.refreshPath,
        data: any(named: 'data'),
      ),
    ).thenAnswer((_) async => _loginResponse);

    final results = await Future.wait([
      repository.refreshSession(),
      repository.refreshSession(),
    ]);

    expect(results[0], same(results[1]));
    verify(
      () => apiClient.postJson(
        AuthApiRepository.refreshPath,
        data: any(named: 'data'),
      ),
    ).called(1);
  });

  test(
    'conditional expiration never clears a different login session',
    () async {
      final newSession = testAuthSession(
        accessToken: 'new-login-access',
        refreshToken: 'new-login-refresh',
      );
      final memoryStorage = _MemoryTokenStorage(newSession);
      repository = AuthApiRepository(apiClient, memoryStorage, now: () => now);

      final cleared = await repository.clearLocalSessionIfCurrent(
        'older-session-id',
      );

      expect(cleared, isFalse);
      expect(memoryStorage.session, same(newSession));
    },
  );

  test('uses the locked password-reset endpoints and DTOs', () async {
    when(
      () => apiClient.postJson(
        AuthApiRepository.resetRequestPath,
        data: any(named: 'data'),
      ),
    ).thenAnswer(
      (_) async => {'challengeId': 'challenge-id', 'expiresIn': 300},
    );
    when(
      () => apiClient.postJson(
        AuthApiRepository.resetVerifyPath,
        data: any(named: 'data'),
      ),
    ).thenAnswer((_) async => {'resetToken': 'reset-token'});
    when(
      () => apiClient.post(
        AuthApiRepository.resetCompletePath,
        data: any(named: 'data'),
      ),
    ).thenAnswer((_) async {});

    final challenge = await repository.requestPasswordReset('0912345678');
    final verification = await repository.verifyPasswordReset(
      challengeId: challenge.challengeId,
      otp: '123456',
    );
    await repository.completePasswordReset(
      resetToken: verification.resetToken,
      newPassword: 'New-password1',
    );

    expect(challenge.expiresIn, 300);
    expect(verification.resetToken, 'reset-token');
    verify(
      () => apiClient.postJson(
        '/api/v1/auth/password-reset/request',
        data: {'phoneNumber': '0912345678'},
      ),
    ).called(1);
    verify(
      () => apiClient.postJson(
        '/api/v1/auth/password-reset/verify',
        data: {'challengeId': 'challenge-id', 'otp': '123456'},
      ),
    ).called(1);
    verify(
      () => apiClient.post(
        '/api/v1/auth/password-reset/complete',
        data: {'resetToken': 'reset-token', 'newPassword': 'New-password1'},
      ),
    ).called(1);
  });
}

const _loginResponse = <String, dynamic>{
  'accessToken': 'access-token',
  'refreshToken': 'refresh-token',
  'expiresIn': 3600,
  'student': {
    'id': 'student-id',
    'studentCode': 'HS001',
    'fullName': 'Nguyễn Văn A',
    'gradeLevel': 10,
    'className': '10A1',
  },
};

const _newLoginResponse = <String, dynamic>{
  'accessToken': 'new-access-token',
  'refreshToken': 'new-refresh-token',
  'expiresIn': 3600,
  'student': {
    'id': 'student-id',
    'studentCode': 'HS001',
    'fullName': 'Nguyễn Văn A',
    'gradeLevel': 10,
    'className': '10A1',
  },
};
