import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/auth_data_providers.dart';
import '../domain/auth_session.dart';
import 'auth_error_message.dart';

enum AuthAction { idle, signingIn, signingOut }

class AuthState {
  const AuthState({
    this.session,
    this.action = AuthAction.idle,
    this.errorMessage,
  });

  final AuthSession? session;
  final AuthAction action;
  final String? errorMessage;

  bool get isAuthenticated => session != null;
  bool get isSigningIn => action == AuthAction.signingIn;
  bool get isSigningOut => action == AuthAction.signingOut;

  AuthState copyWith({
    AuthSession? session,
    bool clearSession = false,
    AuthAction? action,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AuthState(
      session: clearSession ? null : session ?? this.session,
      action: action ?? this.action,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

class AuthController extends AsyncNotifier<AuthState> {
  @override
  Future<AuthState> build() async {
    try {
      final session = await ref.watch(authRepositoryProvider).restoreSession();
      return AuthState(session: session);
    } on Object catch (error) {
      return AuthState(errorMessage: authErrorMessage(error));
    }
  }

  Future<bool> login({
    required String phoneNumber,
    required String password,
  }) async {
    final current = _currentState;
    if (current.isSigningIn) return false;
    state = AsyncData(
      current.copyWith(action: AuthAction.signingIn, clearError: true),
    );
    try {
      final session = await ref
          .read(authRepositoryProvider)
          .login(phoneNumber: phoneNumber, password: password);
      if (!ref.mounted) return false;
      state = AsyncData(AuthState(session: session));
      return true;
    } on Object catch (error) {
      if (!ref.mounted) return false;
      state = AsyncData(AuthState(errorMessage: authErrorMessage(error)));
      return false;
    }
  }

  Future<void> logout() async {
    final current = _currentState;
    if (current.isSigningOut) return;
    state = AsyncData(
      current.copyWith(action: AuthAction.signingOut, clearError: true),
    );
    try {
      await ref.read(authRepositoryProvider).logout();
    } on Object {
      // Local tokens are cleared by the repository even when remote logout
      // fails, so the user must still leave the authenticated area.
    }
    if (ref.mounted) state = const AsyncData(AuthState());
  }

  void adoptRefreshedSession(AuthSession session) {
    final current = _currentState;
    if (current.session?.sessionId != session.sessionId ||
        current.isSigningOut) {
      return;
    }
    state = AsyncData(current.copyWith(session: session));
  }

  Future<void> expireSession(String sessionId) async {
    if (_currentState.session?.sessionId != sessionId) return;
    try {
      await ref
          .read(authRepositoryProvider)
          .clearLocalSessionIfCurrent(sessionId);
    } finally {
      if (ref.mounted && _currentState.session?.sessionId == sessionId) {
        state = const AsyncData(
          AuthState(
            errorMessage: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
          ),
        );
      }
    }
  }

  AuthState get _currentState {
    return switch (state) {
      AsyncData(:final value) => value,
      _ => const AuthState(),
    };
  }
}

final authControllerProvider = AsyncNotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
