import 'dart:async';
import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:myfschoolse1913/src/features/auth/data/auth_token_storage.dart';

import '../../helpers/fake_auth_repository.dart';

class _MockSecureStorage extends Mock implements FlutterSecureStorage {}

void main() {
  test('stores the complete session as one secure value', () async {
    final secureStorage = _MockSecureStorage();
    String? encoded;
    when(
      () => secureStorage.write(
        key: any(named: 'key'),
        value: any(named: 'value'),
      ),
    ).thenAnswer((invocation) async {
      encoded = invocation.namedArguments[#value] as String?;
    });
    final session = testAuthSession();

    await SecureAuthTokenStorage(secureStorage).writeSession(session);

    expect(encoded, contains('access-token'));
    expect(encoded, contains('refresh-token'));
    expect(encoded, contains('Nguyễn Văn A'));
    verify(
      () => secureStorage.write(
        key: 'fpt_schools.auth_session.v1',
        value: any(named: 'value'),
      ),
    ).called(1);
  });

  test('clears malformed persisted session data safely', () async {
    final secureStorage = _MockSecureStorage();
    when(
      () => secureStorage.read(key: any(named: 'key')),
    ).thenAnswer((_) async => '{invalid-json');
    when(
      () => secureStorage.delete(key: any(named: 'key')),
    ).thenAnswer((_) async {});
    final storage = SecureAuthTokenStorage(secureStorage);

    final session = await storage.readSession();

    expect(session, isNull);
    verify(
      () => secureStorage.delete(key: 'fpt_schools.auth_session.v1'),
    ).called(1);
  });

  test('serializes clear after an in-flight secure read', () async {
    final secureStorage = _MockSecureStorage();
    final persistedRead = Completer<String?>();
    when(
      () => secureStorage.read(key: any(named: 'key')),
    ).thenAnswer((_) => persistedRead.future);
    when(
      () => secureStorage.delete(key: any(named: 'key')),
    ).thenAnswer((_) async {});
    final storage = SecureAuthTokenStorage(secureStorage);
    final oldSession = testAuthSession(
      accessToken: 'old-access',
      refreshToken: 'old-refresh',
    );

    final pendingRead = storage.readSession();
    final pendingClear = storage.clear();
    persistedRead.complete(jsonEncode(oldSession.toStorageJson()));

    expect(await pendingRead, isNotNull);
    await pendingClear;
    expect(await storage.readSession(), isNull);
    verifyInOrder([
      () => secureStorage.read(key: 'fpt_schools.auth_session.v1'),
      () => secureStorage.delete(key: 'fpt_schools.auth_session.v1'),
    ]);
  });
}
