import 'dart:async';
import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../domain/auth_session.dart';

abstract interface class AuthTokenStorage {
  Future<AuthSession?> readSession();

  Future<void> writeSession(AuthSession session);

  Future<void> clear();
}

final class SecureAuthTokenStorage implements AuthTokenStorage {
  SecureAuthTokenStorage(this._storage);

  static const _sessionKey = 'fpt_schools.auth_session.v1';

  final FlutterSecureStorage _storage;
  AuthSession? _cachedSession;
  Future<void> _operationTail = Future<void>.value();
  bool _hasLoaded = false;

  @override
  Future<AuthSession?> readSession() {
    return _enqueue(() async {
      if (_hasLoaded) return _cachedSession;
      return _load();
    });
  }

  Future<AuthSession?> _load() async {
    final encoded = await _storage.read(key: _sessionKey);
    if (encoded == null || encoded.isEmpty) {
      _hasLoaded = true;
      return null;
    }
    try {
      final json = jsonDecode(encoded);
      if (json is! Map) {
        throw const FormatException('Invalid stored session JSON.');
      }
      _cachedSession = AuthSession.fromStorageJson(
        Map<String, dynamic>.from(json),
      );
      _hasLoaded = true;
      return _cachedSession;
    } on FormatException {
      await _storage.delete(key: _sessionKey);
      _cachedSession = null;
      _hasLoaded = true;
      return null;
    }
  }

  @override
  Future<void> writeSession(AuthSession session) {
    return _enqueue(() async {
      final encoded = jsonEncode(session.toStorageJson());
      await _storage.write(key: _sessionKey, value: encoded);
      _cachedSession = session;
      _hasLoaded = true;
    });
  }

  @override
  Future<void> clear() {
    return _enqueue(() async {
      await _storage.delete(key: _sessionKey);
      _cachedSession = null;
      _hasLoaded = true;
    });
  }

  Future<T> _enqueue<T>(Future<T> Function() operation) async {
    final previous = _operationTail;
    final completed = Completer<void>();
    _operationTail = completed.future;
    await previous;
    try {
      return await operation();
    } finally {
      completed.complete();
    }
  }
}
