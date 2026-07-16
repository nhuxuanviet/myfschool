import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_interceptor.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_token_storage.dart';
import 'package:myfschoolse1913/src/features/auth/domain/auth_session.dart';

import '../../helpers/fake_auth_repository.dart';

void main() {
  test('adds bearer token and refreshes concurrent 401s only once', () async {
    final oldSession = testAuthSession(accessToken: 'expired-access');
    final newSession = testAuthSession(
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
      sessionId: oldSession.sessionId,
    );
    final storage = _MemoryTokenStorage(oldSession);
    final dio = Dio()..httpClientAdapter = _AuthAdapter();
    addTearDown(() => dio.close(force: true));
    var refreshCalls = 0;
    var expirationCalls = 0;
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: storage,
        refreshSession: () async {
          refreshCalls++;
          await Future<void>.delayed(const Duration(milliseconds: 10));
          await storage.writeSession(newSession);
          return newSession;
        },
        onSessionRefreshed: (_) {},
        onSessionExpired: (_) async {
          expirationCalls++;
        },
      ),
    );

    final responses = await Future.wait([
      dio.get<Map<String, dynamic>>('/protected/grades'),
      dio.get<Map<String, dynamic>>('/protected/schedule'),
    ]);

    expect(refreshCalls, 1);
    expect(expirationCalls, 0);
    expect(responses.every((response) => response.statusCode == 200), isTrue);
  });

  test('signals session expiration once when refresh is rejected', () async {
    final storage = _MemoryTokenStorage(
      testAuthSession(accessToken: 'expired-access'),
    );
    final dio = Dio()..httpClientAdapter = _AuthAdapter();
    addTearDown(() => dio.close(force: true));
    var refreshCalls = 0;
    var expirationCalls = 0;
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: storage,
        refreshSession: () async {
          refreshCalls++;
          await Future<void>.delayed(const Duration(milliseconds: 10));
          return null;
        },
        onSessionRefreshed: (_) {},
        onSessionExpired: (_) async {
          expirationCalls++;
        },
      ),
    );

    final requests = [
      dio.get<Object?>('/protected/grades'),
      dio.get<Object?>('/protected/schedule'),
    ];

    await expectLater(Future.wait(requests), throwsA(isA<DioException>()));
    expect(refreshCalls, 1);
    expect(expirationCalls, 1);
  });

  test('signals expiration again after a new session is stored', () async {
    final storage = _MemoryTokenStorage(
      testAuthSession(accessToken: 'first-expired-access'),
    );
    final dio = Dio()..httpClientAdapter = _AuthAdapter();
    addTearDown(() => dio.close(force: true));
    var expirationCalls = 0;
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: storage,
        refreshSession: () async => null,
        onSessionRefreshed: (_) {},
        onSessionExpired: (_) async {
          expirationCalls++;
          await storage.clear();
        },
      ),
    );

    await expectLater(
      dio.get<Object?>('/protected/grades'),
      throwsA(isA<DioException>()),
    );
    expect(expirationCalls, 1);

    await storage.writeSession(
      testAuthSession(
        accessToken: 'second-expired-access',
        refreshToken: 'second-refresh',
      ),
    );
    await expectLater(
      dio.get<Object?>('/protected/schedule'),
      throwsA(isA<DioException>()),
    );
    expect(expirationCalls, 2);
  });

  test(
    'expires the session when a retried request is still unauthorized',
    () async {
      final storage = _MemoryTokenStorage(
        testAuthSession(accessToken: 'expired-access'),
      );
      final rejectedSession = testAuthSession(
        accessToken: 'still-rejected',
        refreshToken: 'rotated-refresh',
        sessionId: storage.session!.sessionId,
      );
      final dio = Dio()..httpClientAdapter = _AuthAdapter();
      addTearDown(() => dio.close(force: true));
      var expirationCalls = 0;
      dio.interceptors.add(
        AuthInterceptor(
          dio: dio,
          tokenStorage: storage,
          refreshSession: () async {
            await storage.writeSession(rejectedSession);
            return rejectedSession;
          },
          onSessionRefreshed: (_) {},
          onSessionExpired: (_) async {
            expirationCalls++;
            await storage.clear();
          },
        ),
      );

      await expectLater(
        dio.get<Object?>('/protected/grades'),
        throwsA(isA<DioException>()),
      );
      expect(expirationCalls, 1);
      expect(storage.session, isNull);
    },
  );

  test('completes the request when expiration cleanup throws', () async {
    final storage = _MemoryTokenStorage(
      testAuthSession(accessToken: 'expired-access'),
    );
    final dio = Dio()..httpClientAdapter = _AuthAdapter();
    addTearDown(() => dio.close(force: true));
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: storage,
        refreshSession: () async => null,
        onSessionRefreshed: (_) {},
        onSessionExpired: (_) async {
          throw StateError('secure storage unavailable');
        },
      ),
    );

    await expectLater(
      dio.get<Object?>('/protected/grades').timeout(const Duration(seconds: 1)),
      throwsA(isA<DioException>()),
    );
  });

  test('does not refresh or replay a response from an older session', () async {
    final oldSession = testAuthSession(
      accessToken: 'old-access',
      refreshToken: 'old-refresh',
    );
    final storage = _MemoryTokenStorage(oldSession);
    final adapter = _DelayedUnauthorizedAdapter();
    final dio = Dio()..httpClientAdapter = adapter;
    addTearDown(() => dio.close(force: true));
    var refreshCalls = 0;
    var expirationCalls = 0;
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: storage,
        refreshSession: () async {
          refreshCalls++;
          return null;
        },
        onSessionRefreshed: (_) {},
        onSessionExpired: (_) async {
          expirationCalls++;
        },
      ),
    );

    final pendingRequest = dio.get<Object?>('/protected/forms');
    await adapter.requestStarted.future;
    final newSession = testAuthSession(
      accessToken: 'new-login-access',
      refreshToken: 'new-login-refresh',
    );
    await storage.writeSession(newSession);
    adapter.releaseResponse.complete();

    await expectLater(pendingRequest, throwsA(isA<DioException>()));
    expect(refreshCalls, 0);
    expect(expirationCalls, 0);
    expect(adapter.requestCount, 1);
    expect(storage.session, same(newSession));
  });

  test('never rebinds a retry to a newly signed-in session', () async {
    final oldSession = testAuthSession(
      accessToken: 'old-expired-access',
      refreshToken: 'old-refresh',
    );
    final refreshedOldSession = testAuthSession(
      accessToken: 'old-refreshed-access',
      refreshToken: 'old-rotated-refresh',
      sessionId: oldSession.sessionId,
    );
    final newSession = testAuthSession(
      accessToken: 'new-login-access',
      refreshToken: 'new-login-refresh',
    );
    final storage = _MemoryTokenStorage(oldSession);
    final adapter = _RetryRebindingAdapter();
    final dio = Dio()..httpClientAdapter = adapter;
    addTearDown(() => dio.close(force: true));
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: storage,
        refreshSession: () async {
          await storage.writeSession(refreshedOldSession);
          return refreshedOldSession;
        },
        onSessionRefreshed: (_) {
          // Simulate logout followed by a new login in the narrow window
          // between refresh completion and Dio's retry onRequest hook.
          storage.session = newSession;
        },
        onSessionExpired: (_) async {},
      ),
    );

    await expectLater(
      dio.post<Object?>('/protected/forms', data: {'reason': 'leave'}),
      throwsA(
        isA<DioException>().having(
          (error) => error.type,
          'type',
          DioExceptionType.cancel,
        ),
      ),
    );

    expect(adapter.authorizationHeaders, ['Bearer old-expired-access']);
    expect(storage.session, same(newSession));
  });

  test(
    'discards a successful response after the login session changes',
    () async {
      final oldSession = testAuthSession(
        accessToken: 'old-valid-access',
        refreshToken: 'old-refresh',
      );
      final storage = _MemoryTokenStorage(oldSession);
      final adapter = _DelayedSuccessAdapter();
      final dio = Dio()..httpClientAdapter = adapter;
      addTearDown(() => dio.close(force: true));
      var refreshCalls = 0;
      dio.interceptors.add(
        AuthInterceptor(
          dio: dio,
          tokenStorage: storage,
          refreshSession: () async {
            refreshCalls++;
            return null;
          },
          onSessionRefreshed: (_) {},
          onSessionExpired: (_) async {},
        ),
      );

      final pendingRequest = dio.get<Object?>('/protected/grades');
      await adapter.requestStarted.future;
      final newSession = testAuthSession(
        accessToken: 'new-login-access',
        refreshToken: 'new-login-refresh',
      );
      await storage.writeSession(newSession);
      adapter.releaseResponse.complete();

      await expectLater(
        pendingRequest,
        throwsA(
          isA<DioException>().having(
            (error) => error.type,
            'type',
            DioExceptionType.cancel,
          ),
        ),
      );
      expect(refreshCalls, 0);
      expect(storage.session, same(newSession));
    },
  );
}

class _MemoryTokenStorage implements AuthTokenStorage {
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

class _AuthAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final authorization = options.headers['Authorization'];
    if (authorization == 'Bearer new-access') {
      return ResponseBody.fromString(
        '{"ok":true}',
        200,
        headers: {
          Headers.contentTypeHeader: [Headers.jsonContentType],
        },
      );
    }
    return ResponseBody.fromString(
      '{"detail":"expired"}',
      401,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _DelayedUnauthorizedAdapter implements HttpClientAdapter {
  final requestStarted = Completer<void>();
  final releaseResponse = Completer<void>();
  int requestCount = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requestCount++;
    if (!requestStarted.isCompleted) requestStarted.complete();
    await releaseResponse.future;
    return ResponseBody.fromString(
      '{"detail":"expired"}',
      401,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _RetryRebindingAdapter implements HttpClientAdapter {
  final authorizationHeaders = <String?>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    authorizationHeaders.add(options.headers['Authorization']?.toString());
    if (options.headers['Authorization'] == 'Bearer new-login-access') {
      return ResponseBody.fromString('{"ok":true}', 200);
    }
    return ResponseBody.fromString('{"detail":"expired"}', 401);
  }

  @override
  void close({bool force = false}) {}
}

class _DelayedSuccessAdapter implements HttpClientAdapter {
  final requestStarted = Completer<void>();
  final releaseResponse = Completer<void>();

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    if (!requestStarted.isCompleted) requestStarted.complete();
    await releaseResponse.future;
    return ResponseBody.fromString('{"ok":true}', 200);
  }

  @override
  void close({bool force = false}) {}
}
