import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/config/app_environment.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/json_stream_client.dart';
import 'auth_interceptor.dart';
import 'auth_token_storage.dart';

final class AuthenticatedJsonStreamClient implements JsonStreamClient {
  AuthenticatedJsonStreamClient({
    required AuthTokenStorage tokenStorage,
    required RefreshSession refreshSession,
    required SessionRefreshed onSessionRefreshed,
    required SessionExpired onSessionExpired,
    http.Client? httpClient,
  }) : _tokenStorage = tokenStorage,
       _refreshSession = refreshSession,
       _onSessionRefreshed = onSessionRefreshed,
       _onSessionExpired = onSessionExpired,
       _httpClient = httpClient ?? http.Client();

  static const _ndjsonContentType = 'application/x-ndjson';

  final AuthTokenStorage _tokenStorage;
  final RefreshSession _refreshSession;
  final SessionRefreshed _onSessionRefreshed;
  final SessionExpired _onSessionExpired;
  final http.Client _httpClient;

  void close() => _httpClient.close();

  @override
  Stream<List<int>> postJsonStream(
    String path, {
    required Map<String, dynamic> data,
  }) async* {
    final initialSession = await _tokenStorage.readSession();
    if (initialSession == null) {
      throw const ApiException(
        message: 'Authentication is required.',
        statusCode: 401,
      );
    }

    var session = initialSession;
    var response = await _send(path, data, session.tokens.accessToken);
    if (response.statusCode == 401) {
      await response.stream.drain<void>();
      final refreshed = await _refreshSession();
      if (refreshed == null ||
          refreshed.sessionId != initialSession.sessionId) {
        await _onSessionExpired(initialSession.sessionId);
        throw const ApiException(
          message: 'The authentication session has expired.',
          statusCode: 401,
        );
      }
      session = refreshed;
      _onSessionRefreshed(refreshed);
      response = await _send(path, data, session.tokens.accessToken);
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw await _apiException(response);
    }
    if (!await _isCurrentSession(session.sessionId)) {
      throw const ApiException(message: 'Authentication session changed.');
    }
    yield* response.stream;
  }

  Future<http.StreamedResponse> _send(
    String path,
    Map<String, dynamic> data,
    String accessToken,
  ) {
    final request =
        http.Request('POST', Uri.parse('${AppEnvironment.apiBaseUrl}$path'))
          ..headers.addAll({
            'Accept': _ndjsonContentType,
            'Authorization': 'Bearer $accessToken',
            'Content-Type': 'application/json',
          })
          ..body = jsonEncode(data);
    return _httpClient.send(request);
  }

  Future<bool> _isCurrentSession(String sessionId) async {
    final current = await _tokenStorage.readSession();
    return current?.sessionId == sessionId;
  }

  static Future<ApiException> _apiException(
    http.StreamedResponse response,
  ) async {
    final body = await response.stream.bytesToString();
    Map<String, dynamic> problem = const {};
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map) {
        problem = Map<String, dynamic>.from(decoded);
      }
    } on FormatException {
      // Preserve the HTTP status when the upstream did not return ProblemDetail.
    }
    return ApiException(
      message:
          problem['detail']?.toString() ??
          problem['title']?.toString() ??
          'The streaming request could not be completed.',
      statusCode: response.statusCode,
      code: problem['code']?.toString(),
      instance: problem['instance']?.toString(),
    );
  }
}
