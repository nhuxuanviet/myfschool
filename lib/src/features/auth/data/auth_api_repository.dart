import 'dart:async';
import 'dart:convert';
import 'dart:math';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../domain/auth_repository.dart';
import '../domain/auth_session.dart';
import '../domain/password_reset.dart';
import 'auth_token_storage.dart';

final class AuthApiRepository implements AuthRepository {
  AuthApiRepository(
    this._apiClient,
    this._tokenStorage, {
    DateTime Function()? now,
    String Function()? sessionIdFactory,
  }) : _now = now ?? DateTime.now,
       _sessionIdFactory = sessionIdFactory ?? _generateSessionId;

  static const loginPath = '/api/v1/auth/login';
  static const refreshPath = '/api/v1/auth/refresh';
  static const logoutPath = '/api/v1/auth/logout';
  static const resetRequestPath = '/api/v1/auth/password-reset/request';
  static const resetVerifyPath = '/api/v1/auth/password-reset/verify';
  static const resetCompletePath = '/api/v1/auth/password-reset/complete';

  final ApiClient _apiClient;
  final AuthTokenStorage _tokenStorage;
  final DateTime Function() _now;
  final String Function() _sessionIdFactory;
  Future<void> _sessionMutationTail = Future<void>.value();
  Future<AuthSession?>? _refreshing;
  int _sessionGeneration = 0;

  @override
  Future<AuthSession> login({
    required String phoneNumber,
    required String password,
  }) async {
    final json = await _apiClient.postJson(
      loginPath,
      data: {'phoneNumber': phoneNumber, 'password': password},
    );
    final session = AuthSession.fromLoginJson(
      json,
      now: _now(),
      sessionId: _sessionIdFactory(),
    );
    await _withSessionMutation(() async {
      _sessionGeneration++;
      await _tokenStorage.writeSession(session);
    });
    return session;
  }

  @override
  Future<AuthSession?> restoreSession() async {
    final session = await _withSessionMutation(_tokenStorage.readSession);
    if (session == null) return null;
    if (!session.tokens.isExpired(_now())) return session;
    return refreshSession();
  }

  @override
  Future<AuthSession?> refreshSession() {
    final inProgress = _refreshing;
    if (inProgress != null) return inProgress;

    late final Future<AuthSession?> refresh;
    refresh = _refreshSession().whenComplete(() {
      if (identical(_refreshing, refresh)) _refreshing = null;
    });
    _refreshing = refresh;
    return refresh;
  }

  Future<AuthSession?> _refreshSession() async {
    final attempt = await _withSessionMutation(() async {
      return _RefreshAttempt(
        generation: _sessionGeneration,
        session: await _tokenStorage.readSession(),
      );
    });
    final current = attempt.session;
    if (current == null) return null;

    try {
      final json = await _apiClient.postJson(
        refreshPath,
        data: {'refreshToken': current.tokens.refreshToken},
      );
      final refreshed = AuthSession.fromLoginJson(
        json,
        now: _now(),
        sessionId: current.sessionId,
      );
      return _withSessionMutation(() async {
        if (attempt.generation != _sessionGeneration) {
          return _tokenStorage.readSession();
        }
        await _tokenStorage.writeSession(refreshed);
        return refreshed;
      });
    } on ApiException catch (error) {
      if (_invalidatesSession(error)) {
        return _invalidateSession(attempt.generation);
      }
      rethrow;
    } on FormatException {
      return _invalidateSession(attempt.generation);
    }
  }

  bool _invalidatesSession(ApiException error) {
    return error.statusCode == 401 ||
        error.statusCode == 403 ||
        const {
          'INVALID_REFRESH_TOKEN',
          'REFRESH_TOKEN_REUSED',
          'SESSION_REVOKED',
        }.contains(error.code);
  }

  @override
  Future<void> logout() async {
    final session = await _withSessionMutation(() async {
      _sessionGeneration++;
      AuthSession? current;
      try {
        current = await _tokenStorage.readSession();
      } finally {
        await _tokenStorage.clear();
      }
      return current;
    });
    if (session != null) {
      await _apiClient.post(
        logoutPath,
        data: {'refreshToken': session.tokens.refreshToken},
      );
    }
  }

  @override
  Future<void> clearLocalSession() {
    return _withSessionMutation(() async {
      _sessionGeneration++;
      await _tokenStorage.clear();
    });
  }

  @override
  Future<bool> clearLocalSessionIfCurrent(String sessionId) {
    return _withSessionMutation(() async {
      final current = await _tokenStorage.readSession();
      if (current != null && current.sessionId != sessionId) return false;
      if (current != null) {
        _sessionGeneration++;
        await _tokenStorage.clear();
      }
      return true;
    });
  }

  @override
  Future<PasswordResetChallenge> requestPasswordReset(
    String phoneNumber,
  ) async {
    final json = await _apiClient.postJson(
      resetRequestPath,
      data: {'phoneNumber': phoneNumber},
    );
    return PasswordResetChallenge.fromJson(json);
  }

  @override
  Future<PasswordResetVerification> verifyPasswordReset({
    required String challengeId,
    required String otp,
  }) async {
    final json = await _apiClient.postJson(
      resetVerifyPath,
      data: {'challengeId': challengeId, 'otp': otp},
    );
    return PasswordResetVerification.fromJson(json);
  }

  @override
  Future<void> completePasswordReset({
    required String resetToken,
    required String newPassword,
  }) {
    return _apiClient.post(
      resetCompletePath,
      data: {'resetToken': resetToken, 'newPassword': newPassword},
    );
  }

  Future<AuthSession?> _invalidateSession(int expectedGeneration) {
    return _withSessionMutation(() async {
      if (expectedGeneration != _sessionGeneration) {
        return _tokenStorage.readSession();
      }
      _sessionGeneration++;
      await _tokenStorage.clear();
      return null;
    });
  }

  Future<T> _withSessionMutation<T>(Future<T> Function() operation) async {
    final previous = _sessionMutationTail;
    final completed = Completer<void>();
    _sessionMutationTail = completed.future;
    await previous;
    try {
      return await operation();
    } finally {
      completed.complete();
    }
  }
}

String _generateSessionId() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  return base64Url.encode(bytes).replaceAll('=', '');
}

final class _RefreshAttempt {
  const _RefreshAttempt({required this.generation, required this.session});

  final int generation;
  final AuthSession? session;
}
