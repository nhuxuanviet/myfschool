class PasswordResetChallenge {
  const PasswordResetChallenge({
    required this.challengeId,
    required this.expiresIn,
  });

  factory PasswordResetChallenge.fromJson(Map<String, dynamic> json) {
    final challengeId = json['challengeId']?.toString().trim();
    final rawExpiresIn = json['expiresIn'];
    final expiresIn = rawExpiresIn is num
        ? rawExpiresIn.toInt()
        : int.tryParse(rawExpiresIn?.toString() ?? '');
    if (challengeId == null || challengeId.isEmpty) {
      throw const FormatException('Missing password reset challenge ID.');
    }
    if (expiresIn == null || expiresIn <= 0) {
      throw const FormatException('Invalid password reset expiration.');
    }
    return PasswordResetChallenge(
      challengeId: challengeId,
      expiresIn: expiresIn,
    );
  }

  final String challengeId;
  final int expiresIn;
}

class PasswordResetVerification {
  const PasswordResetVerification({required this.resetToken});

  factory PasswordResetVerification.fromJson(Map<String, dynamic> json) {
    final resetToken = json['resetToken']?.toString().trim();
    if (resetToken == null || resetToken.isEmpty) {
      throw const FormatException('Missing password reset token.');
    }
    return PasswordResetVerification(resetToken: resetToken);
  }

  final String resetToken;
}
