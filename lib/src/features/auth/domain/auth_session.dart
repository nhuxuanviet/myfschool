class StudentSummary {
  const StudentSummary({
    required this.id,
    required this.studentCode,
    required this.fullName,
    required this.gradeLevel,
    required this.className,
  });

  factory StudentSummary.fromJson(Map<String, dynamic> json) {
    return StudentSummary(
      id: _requiredString(json, 'id'),
      studentCode: _requiredString(json, 'studentCode'),
      fullName: _requiredString(json, 'fullName'),
      gradeLevel: _requiredInt(json, 'gradeLevel'),
      className: _requiredString(json, 'className'),
    );
  }

  final String id;
  final String studentCode;
  final String fullName;
  final int gradeLevel;
  final String className;

  Map<String, dynamic> toJson() => {
    'id': id,
    'studentCode': studentCode,
    'fullName': fullName,
    'gradeLevel': gradeLevel,
    'className': className,
  };
}

class AuthTokens {
  const AuthTokens({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresAt,
  });

  factory AuthTokens.fromApiJson(
    Map<String, dynamic> json, {
    required DateTime now,
  }) {
    final expiresIn = _requiredPositiveInt(json, 'expiresIn');
    return AuthTokens(
      accessToken: _requiredString(json, 'accessToken'),
      refreshToken: _requiredString(json, 'refreshToken'),
      expiresAt: now.toUtc().add(Duration(seconds: expiresIn)),
    );
  }

  factory AuthTokens.fromStorageJson(Map<String, dynamic> json) {
    final expiresAt = DateTime.tryParse(_requiredString(json, 'expiresAt'));
    if (expiresAt == null) {
      throw const FormatException('Invalid token expiration timestamp.');
    }
    return AuthTokens(
      accessToken: _requiredString(json, 'accessToken'),
      refreshToken: _requiredString(json, 'refreshToken'),
      expiresAt: expiresAt.toUtc(),
    );
  }

  final String accessToken;
  final String refreshToken;
  final DateTime expiresAt;

  bool isExpired(DateTime now, {Duration skew = const Duration(seconds: 30)}) {
    return !now.toUtc().add(skew).isBefore(expiresAt);
  }

  Map<String, dynamic> toStorageJson() => {
    'accessToken': accessToken,
    'refreshToken': refreshToken,
    'expiresAt': expiresAt.toUtc().toIso8601String(),
  };
}

/// Which app a signed-in account opens into.
///
/// The account decides this, not the person: a phone number issued to a teacher
/// signs in as a teacher. There is no role picker.
enum AppRole {
  student,
  teacher,
  parent;

  static AppRole fromName(String value) {
    return switch (value.toUpperCase()) {
      'TEACHER' => AppRole.teacher,
      'PARENT' => AppRole.parent,
      _ => AppRole.student,
    };
  }

  String get wireName => switch (this) {
    AppRole.student => 'STUDENT',
    AppRole.teacher => 'TEACHER',
    AppRole.parent => 'PARENT',
  };
}

class AuthSession {
  const AuthSession({
    required this.sessionId,
    required this.tokens,
    required this.role,
    this.student,
  });

  factory AuthSession.fromLoginJson(
    Map<String, dynamic> json, {
    required DateTime now,
    String? sessionId,
  }) {
    final tokens = AuthTokens.fromApiJson(json, now: now);
    final role = AppRole.fromName(json['activeRole']?.toString() ?? 'STUDENT');
    final studentJson = json['student'];
    // Only a student session carries a student profile. A teacher or guardian
    // session legitimately has none, and demanding one here is what would make
    // their sign-in fail on a field they were never going to have.
    if (role == AppRole.student && studentJson is! Map) {
      throw const FormatException('Missing student profile.');
    }
    return AuthSession(
      sessionId: sessionId ?? tokens.refreshToken,
      tokens: tokens,
      role: role,
      student: studentJson is Map
          ? StudentSummary.fromJson(Map<String, dynamic>.from(studentJson))
          : null,
    );
  }

  factory AuthSession.fromStorageJson(Map<String, dynamic> json) {
    final tokenJson = json['tokens'];
    if (tokenJson is! Map) {
      throw const FormatException('Invalid stored authentication session.');
    }
    final tokens = AuthTokens.fromStorageJson(
      Map<String, dynamic>.from(tokenJson),
    );
    final role = AppRole.fromName(json['role']?.toString() ?? 'STUDENT');
    final studentJson = json['student'];
    if (role == AppRole.student && studentJson is! Map) {
      throw const FormatException('Invalid stored authentication session.');
    }
    final storedSessionId = json['sessionId']?.toString().trim();
    return AuthSession(
      sessionId: storedSessionId == null || storedSessionId.isEmpty
          ? tokens.refreshToken
          : storedSessionId,
      tokens: tokens,
      role: role,
      student: studentJson is Map
          ? StudentSummary.fromJson(Map<String, dynamic>.from(studentJson))
          : null,
    );
  }

  final String sessionId;
  final AuthTokens tokens;
  final AppRole role;

  /// Null for a teacher or guardian session.
  final StudentSummary? student;

  /// The student of a student session. Calling this on another role is a bug.
  StudentSummary get requireStudent {
    final value = student;
    if (value == null) {
      throw StateError('This session is not a student session.');
    }
    return value;
  }

  AuthSession copyWithTokens(AuthTokens value) {
    return AuthSession(
      sessionId: sessionId,
      tokens: value,
      role: role,
      student: student,
    );
  }

  Map<String, dynamic> toStorageJson() => {
    'sessionId': sessionId,
    'tokens': tokens.toStorageJson(),
    'role': role.wireName,
    if (student != null) 'student': student!.toJson(),
  };
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key]?.toString().trim();
  if (value == null || value.isEmpty) {
    throw FormatException('Missing required field: $key.');
  }
  return value;
}

int _requiredPositiveInt(Map<String, dynamic> json, String key) {
  final raw = json[key];
  final value = raw is num ? raw.toInt() : int.tryParse(raw?.toString() ?? '');
  if (value == null || value <= 0) {
    throw FormatException('Invalid positive integer field: $key.');
  }
  return value;
}

int _requiredInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value <= 0) {
    throw FormatException('Invalid integer field: $key.');
  }
  return value;
}
