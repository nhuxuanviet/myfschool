import 'package:flutter/foundation.dart';

abstract final class AppEnvironment {
  static const _configuredApiBaseUrl = String.fromEnvironment('API_BASE_URL');

  static String get apiBaseUrl {
    final configured = _configuredApiBaseUrl.trim();
    if (configured.isEmpty && kReleaseMode) {
      throw const FormatException(
        'API_BASE_URL is required for release builds.',
      );
    }

    final value = configured.isNotEmpty ? configured : _developmentBaseUrl;
    final uri = Uri.tryParse(value);
    final validScheme = uri?.scheme == 'http' || uri?.scheme == 'https';
    if (uri == null ||
        !validScheme ||
        uri.host.isEmpty ||
        uri.userInfo.isNotEmpty ||
        uri.hasQuery ||
        uri.hasFragment) {
      throw const FormatException(
        'API_BASE_URL must be an HTTP(S) base URL without credentials, query, or fragment.',
      );
    }
    final isLoopback = const {
      'localhost',
      '127.0.0.1',
      '::1',
    }.contains(uri.host.toLowerCase());
    if (kReleaseMode && uri.scheme != 'https' && !isLoopback) {
      throw const FormatException(
        'Release builds require an HTTPS API_BASE_URL outside localhost.',
      );
    }

    final normalizedPath = uri.path == '/' ? '' : uri.path;
    return uri
        .replace(path: normalizedPath)
        .toString()
        .replaceFirst(RegExp(r'/$'), '');
  }

  static String get _developmentBaseUrl {
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }
}
