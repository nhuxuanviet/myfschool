final class HomeDashboard {
  const HomeDashboard({
    required this.student,
    required this.academicTerm,
    required this.summary,
    required this.announcements,
  });

  factory HomeDashboard.fromJson(Map<String, dynamic> json) {
    final rawAnnouncements = _requiredList(json, 'announcements');
    final academicTermJson = _optionalMap(json, 'academicTerm');
    return HomeDashboard(
      student: HomeStudent.fromJson(_requiredMap(json, 'student')),
      academicTerm: academicTermJson == null
          ? null
          : AcademicTerm.fromJson(academicTermJson),
      summary: HomeSummary.fromJson(_requiredMap(json, 'summary')),
      announcements: rawAnnouncements
          .map((item) {
            if (item is! Map) {
              throw const FormatException('Invalid announcement item.');
            }
            return HomeAnnouncement.fromJson(Map<String, dynamic>.from(item));
          })
          .toList(growable: false),
    );
  }

  final HomeStudent student;
  final AcademicTerm? academicTerm;
  final HomeSummary summary;
  final List<HomeAnnouncement> announcements;
}

final class HomeStudent {
  const HomeStudent({
    required this.studentCode,
    required this.fullName,
    required this.gradeLevel,
    required this.className,
  });

  factory HomeStudent.fromJson(Map<String, dynamic> json) {
    return HomeStudent(
      studentCode: _requiredString(json, 'studentCode'),
      fullName: _requiredString(json, 'fullName'),
      gradeLevel: _requiredPositiveInt(json, 'gradeLevel'),
      className: _requiredString(json, 'className'),
    );
  }

  final String studentCode;
  final String fullName;
  final int gradeLevel;
  final String className;
}

final class AcademicTerm {
  const AcademicTerm({
    required this.academicYear,
    required this.code,
    required this.name,
    required this.startsOn,
    required this.endsOn,
  });

  factory AcademicTerm.fromJson(Map<String, dynamic> json) {
    return AcademicTerm(
      academicYear: _requiredString(json, 'academicYear'),
      code: _requiredString(json, 'code'),
      name: _requiredString(json, 'name'),
      startsOn: _requiredDate(json, 'startsOn'),
      endsOn: _requiredDate(json, 'endsOn'),
    );
  }

  final String academicYear;
  final String code;
  final String name;
  final DateTime startsOn;
  final DateTime endsOn;
}

final class HomeSummary {
  const HomeSummary({
    required this.todayLessons,
    required this.upcomingEvents,
    required this.pendingForms,
    required this.activeClubs,
  });

  factory HomeSummary.fromJson(Map<String, dynamic> json) {
    return HomeSummary(
      todayLessons: _requiredNonNegativeInt(
        _requiredMap(json, 'lessons'),
        'today',
      ),
      upcomingEvents: _requiredNonNegativeInt(
        _requiredMap(json, 'events'),
        'upcoming',
      ),
      pendingForms: _requiredNonNegativeInt(
        _requiredMap(json, 'forms'),
        'pending',
      ),
      activeClubs: _requiredNonNegativeInt(
        _requiredMap(json, 'clubs'),
        'active',
      ),
    );
  }

  final int todayLessons;
  final int upcomingEvents;
  final int pendingForms;
  final int activeClubs;
}

final class HomeAnnouncement {
  const HomeAnnouncement({
    required this.id,
    required this.title,
    required this.body,
    required this.publishedAt,
  });

  factory HomeAnnouncement.fromJson(Map<String, dynamic> json) {
    return HomeAnnouncement(
      id: _requiredString(json, 'id'),
      title: _requiredString(json, 'title'),
      body: _requiredString(json, 'body'),
      publishedAt: _requiredDateTime(json, 'publishedAt'),
    );
  }

  final String id;
  final String title;
  final String body;
  final DateTime publishedAt;
}

Map<String, dynamic> _requiredMap(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! Map) {
    throw FormatException('Missing required object: $key.');
  }
  return Map<String, dynamic>.from(value);
}

Map<String, dynamic>? _optionalMap(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value == null) return null;
  if (value is! Map) {
    throw FormatException('Invalid object: $key.');
  }
  return Map<String, dynamic>.from(value);
}

List<Object?> _requiredList(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! List) {
    throw FormatException('Missing required list: $key.');
  }
  return List<Object?>.from(value);
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key]?.toString().trim();
  if (value == null || value.isEmpty) {
    throw FormatException('Missing required field: $key.');
  }
  return value;
}

int _requiredPositiveInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value <= 0) {
    throw FormatException('Invalid positive integer field: $key.');
  }
  return value;
}

int _requiredNonNegativeInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value < 0) {
    throw FormatException('Invalid non-negative integer field: $key.');
  }
  return value;
}

DateTime _requiredDate(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  if (!RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(value)) {
    throw FormatException('Invalid date field: $key.');
  }
  final parsed = DateTime.tryParse(value);
  if (parsed == null) throw FormatException('Invalid date field: $key.');
  return parsed;
}

DateTime _requiredDateTime(Map<String, dynamic> json, String key) {
  final parsed = DateTime.tryParse(_requiredString(json, key));
  if (parsed == null) throw FormatException('Invalid date-time field: $key.');
  return parsed.toLocal();
}
