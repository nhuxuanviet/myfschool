import 'dart:async';

import 'package:dio/dio.dart';

import '../domain/auth_session.dart';
import 'auth_token_storage.dart';

typedef RefreshSession = Future<AuthSession?> Function();
typedef SessionRefreshed = void Function(AuthSession session);
typedef SessionExpired = Future<void> Function(String sessionId);

final class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required Dio dio,
    required AuthTokenStorage tokenStorage,
    required RefreshSession refreshSession,
    required SessionRefreshed onSessionRefreshed,
    required SessionExpired onSessionExpired,
  }) : _dio = dio,
       _tokenStorage = tokenStorage,
       _refreshSession = refreshSession,
       _onSessionRefreshed = onSessionRefreshed,
       _onSessionExpired = onSessionExpired;

  static const _authorizationHeader = 'Authorization';
  static const _retryKey = 'auth.refresh.retry';
  static const _sessionIdKey = 'auth.session.id';

  final Dio _dio;
  final AuthTokenStorage _tokenStorage;
  final RefreshSession _refreshSession;
  final SessionRefreshed _onSessionRefreshed;
  final SessionExpired _onSessionExpired;

  Future<AuthSession?>? _refreshing;
  String? _expirationSignaledFor;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (_isProtectedEndpoint(options.path) &&
        options.extra[_retryKey] == true) {
      final boundSessionId = options.extra[_sessionIdKey];
      if (boundSessionId is! String ||
          !await _isCurrentSession(boundSessionId)) {
        handler.reject(_sessionChanged(options));
        return;
      }

      // A retry is permanently bound to the session that originated the
      // request. Re-reading and replacing its token here could replay an old
      // request as a newly signed-in user.
      handler.next(options);
      return;
    }

    if (_isProtectedEndpoint(options.path)) {
      try {
        final session = await _tokenStorage.readSession();
        if (session != null) {
          if (_expirationSignaledFor != session.sessionId) {
            _expirationSignaledFor = null;
          }
          options.extra[_sessionIdKey] = session.sessionId;
          options.headers[_authorizationHeader] =
              'Bearer ${session.tokens.accessToken}';
        }
      } on Object {
        // A storage failure must not deadlock Dio's interceptor queue. The
        // server will reject the unauthenticated request if auth is required.
      }
    }
    handler.next(options);
  }

  @override
  void onResponse(
    Response<Object?> response,
    ResponseInterceptorHandler handler,
  ) async {
    final request = response.requestOptions;
    final requestSessionId = request.extra[_sessionIdKey];
    if (_isProtectedEndpoint(request.path) &&
        requestSessionId is String &&
        !await _isCurrentSession(requestSessionId)) {
      // Never expose an in-flight response from an older login session to the
      // state/cache owned by the current user.
      handler.reject(_sessionChanged(request, response: response));
      return;
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final request = err.requestOptions;
    final requestSessionId = request.extra[_sessionIdKey];
    if (err.response?.statusCode != 401 ||
        request.extra[_retryKey] == true ||
        requestSessionId is! String ||
        _isPublicAuthEndpoint(request.path)) {
      handler.next(err);
      return;
    }

    if (!await _isCurrentSession(requestSessionId)) {
      handler.next(err);
      return;
    }

    AuthSession? refreshed;
    try {
      refreshed = await _refreshSingleFlight();
    } on Object {
      // Preserve the original protected-resource failure when refresh could
      // not be attempted because of a transient transport error.
      handler.next(err);
      return;
    }

    if (refreshed == null) {
      await _signalExpirationSafely(requestSessionId);
      handler.next(err);
      return;
    }
    if (refreshed.sessionId != requestSessionId) {
      handler.next(err);
      return;
    }

    _expirationSignaledFor = null;
    try {
      _onSessionRefreshed(refreshed);
    } on Object {
      // State propagation must not prevent the original request from being
      // retried with the token that was already persisted successfully.
    }
    final headers = Map<String, dynamic>.from(request.headers)
      ..[_authorizationHeader] = 'Bearer ${refreshed.tokens.accessToken}';
    final extra = Map<String, dynamic>.from(request.extra)..[_retryKey] = true;

    try {
      final response = await _dio.fetch<Object?>(
        request.copyWith(headers: headers, extra: extra),
      );
      handler.resolve(response);
    } on DioException catch (retryError) {
      if (retryError.response?.statusCode == 401) {
        await _signalExpirationSafely(requestSessionId);
      }
      handler.next(retryError);
    }
  }

  Future<AuthSession?> _refreshSingleFlight() {
    final inProgress = _refreshing;
    if (inProgress != null) return inProgress;

    late final Future<AuthSession?> refresh;
    refresh = _refreshSession().whenComplete(() {
      if (identical(_refreshing, refresh)) _refreshing = null;
    });
    _refreshing = refresh;
    return refresh;
  }

  Future<bool> _isCurrentSession(String sessionId) async {
    try {
      final current = await _tokenStorage.readSession();
      return current?.sessionId == sessionId;
    } on Object {
      return false;
    }
  }

  Future<void> _signalExpirationOnce(String sessionId) async {
    if (_expirationSignaledFor == sessionId) return;
    _expirationSignaledFor = sessionId;
    await _onSessionExpired(sessionId);
  }

  Future<void> _signalExpirationSafely(String sessionId) async {
    try {
      await _signalExpirationOnce(sessionId);
    } on Object {
      // Dio's interceptor contract still requires resolving the handler when
      // secure storage cleanup or state propagation fails.
    }
  }

  DioException _sessionChanged(
    RequestOptions request, {
    Response<Object?>? response,
  }) {
    return DioException(
      requestOptions: request,
      response: response,
      type: DioExceptionType.cancel,
      message: 'Authentication session changed while the request was active.',
    );
  }

  bool _isProtectedEndpoint(String path) => !_isPublicAuthEndpoint(path);

  bool _isPublicAuthEndpoint(String path) {
    final normalizedPath = Uri.tryParse(path)?.path ?? path;
    return normalizedPath == '/api/v1/auth/login' ||
        normalizedPath == '/api/v1/auth/refresh' ||
        normalizedPath.startsWith('/api/v1/auth/password-reset/');
  }
}
