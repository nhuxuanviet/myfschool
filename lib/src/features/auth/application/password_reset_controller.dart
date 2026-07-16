import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/auth_data_providers.dart';
import 'auth_error_message.dart';

enum PasswordResetStep { phone, otp, newPassword, success }

class PasswordResetState {
  const PasswordResetState({
    this.step = PasswordResetStep.phone,
    this.phoneNumber,
    this.challengeId,
    this.resetToken,
    this.expiresIn,
    this.isLoading = false,
    this.errorMessage,
  });

  final PasswordResetStep step;
  final String? phoneNumber;
  final String? challengeId;
  final String? resetToken;
  final int? expiresIn;
  final bool isLoading;
  final String? errorMessage;

  PasswordResetState copyWith({
    PasswordResetStep? step,
    String? phoneNumber,
    String? challengeId,
    String? resetToken,
    int? expiresIn,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
  }) {
    return PasswordResetState(
      step: step ?? this.step,
      phoneNumber: phoneNumber ?? this.phoneNumber,
      challengeId: challengeId ?? this.challengeId,
      resetToken: resetToken ?? this.resetToken,
      expiresIn: expiresIn ?? this.expiresIn,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

class PasswordResetController extends Notifier<PasswordResetState> {
  @override
  PasswordResetState build() => const PasswordResetState();

  Future<bool> requestOtp(String phoneNumber) async {
    if (state.isLoading) return false;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final challenge = await ref
          .read(authRepositoryProvider)
          .requestPasswordReset(phoneNumber);
      if (!ref.mounted) return false;
      state = PasswordResetState(
        step: PasswordResetStep.otp,
        phoneNumber: phoneNumber,
        challengeId: challenge.challengeId,
        expiresIn: challenge.expiresIn,
      );
      return true;
    } on Object catch (error) {
      if (!ref.mounted) return false;
      state = PasswordResetState(
        phoneNumber: phoneNumber,
        errorMessage: authErrorMessage(error),
      );
      return false;
    }
  }

  Future<bool> verifyOtp(String otp) async {
    final challengeId = state.challengeId;
    if (state.isLoading || challengeId == null) return false;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final verification = await ref
          .read(authRepositoryProvider)
          .verifyPasswordReset(challengeId: challengeId, otp: otp);
      if (!ref.mounted) return false;
      state = state.copyWith(
        step: PasswordResetStep.newPassword,
        resetToken: verification.resetToken,
        isLoading: false,
        clearError: true,
      );
      return true;
    } on Object catch (error) {
      if (!ref.mounted) return false;
      state = state.copyWith(
        isLoading: false,
        errorMessage: authErrorMessage(error),
      );
      return false;
    }
  }

  Future<bool> complete(String newPassword) async {
    final resetToken = state.resetToken;
    if (state.isLoading || resetToken == null) return false;
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await ref
          .read(authRepositoryProvider)
          .completePasswordReset(
            resetToken: resetToken,
            newPassword: newPassword,
          );
      if (!ref.mounted) return false;
      state = const PasswordResetState(step: PasswordResetStep.success);
      return true;
    } on Object catch (error) {
      if (!ref.mounted) return false;
      state = state.copyWith(
        isLoading: false,
        errorMessage: authErrorMessage(error),
      );
      return false;
    }
  }

  void returnToPhone() {
    if (!state.isLoading) state = const PasswordResetState();
  }
}

final passwordResetControllerProvider =
    NotifierProvider.autoDispose<PasswordResetController, PasswordResetState>(
      PasswordResetController.new,
    );
