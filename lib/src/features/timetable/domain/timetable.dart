/// A school-week timetable returned by the authenticated student API.
///
/// Dates intentionally use UTC midnight because they represent school calendar
/// dates, not instants in time. The server supplies all displayed lesson times
/// as local `HH:mm` values in [timeZone].
final class Timetable {
  const Timetable({
    required this.timeZone,
    required this.weekStart,
    required this.weekEnd,
    required this.academicTerms,
    required this.days,
  });

  factory Timetable.fromJson(Map<String, dynamic> json) {
    final weekStart = _requiredIsoDate(json, 'weekStart');
    final weekEnd = _requiredIsoDate(json, 'weekEnd');
    final rawTerms = _requiredList(json, 'academicTerms');
    final rawDays = _requiredList(json, 'days');

    if (weekStart.weekday != DateTime.monday) {
      throw const FormatException('weekStart must be a Monday.');
    }
    if (!_isSameDate(weekEnd, weekStart.add(const Duration(days: 6)))) {
      throw const FormatException('weekEnd must be six days after weekStart.');
    }
    if (rawDays.length != TimetableDayOfWeek.values.length) {
      throw const FormatException('A timetable week must contain seven days.');
    }

    final days = List<TimetableDay>.generate(rawDays.length, (index) {
      final rawDay = rawDays[index];
      if (rawDay is! Map) {
        throw const FormatException('Invalid timetable day.');
      }
      final day = TimetableDay.fromJson(Map<String, dynamic>.from(rawDay));
      final expectedDate = weekStart.add(Duration(days: index));
      final expectedDayOfWeek = TimetableDayOfWeek.values[index];
      if (!_isSameDate(day.date, expectedDate) ||
          day.dayOfWeek != expectedDayOfWeek) {
        throw const FormatException('Timetable days must be consecutive.');
      }
      return day;
    }, growable: false);
    final academicTerms = rawTerms
        .map((rawTerm) {
          if (rawTerm is! Map) {
            throw const FormatException('Invalid academic term.');
          }
          return TimetableAcademicTerm.fromJson(
            Map<String, dynamic>.from(rawTerm),
          );
        })
        .toList(growable: false);
    for (var index = 0; index < academicTerms.length; index += 1) {
      final term = academicTerms[index];
      if (!_overlapsWeek(term, weekStart, weekEnd)) {
        throw const FormatException(
          'Each academic term must overlap the requested week.',
        );
      }
      if (index > 0 &&
          _compareAcademicTerms(academicTerms[index - 1], term) > 0) {
        throw const FormatException(
          'Academic terms must be ordered by start date, end date, and code.',
        );
      }
    }
    _validateLessonCoverage(days, academicTerms);

    return Timetable(
      timeZone: _requiredString(json, 'timeZone'),
      weekStart: weekStart,
      weekEnd: weekEnd,
      academicTerms: academicTerms,
      days: days,
    );
  }

  final String timeZone;
  final DateTime weekStart;
  final DateTime weekEnd;
  final List<TimetableAcademicTerm> academicTerms;
  final List<TimetableDay> days;
}

final class TimetableAcademicTerm {
  const TimetableAcademicTerm({
    required this.academicYear,
    required this.code,
    required this.name,
    required this.startsOn,
    required this.endsOn,
  });

  factory TimetableAcademicTerm.fromJson(Map<String, dynamic> json) {
    final startsOn = _requiredIsoDate(json, 'startsOn');
    final endsOn = _requiredIsoDate(json, 'endsOn');
    if (startsOn.isAfter(endsOn)) {
      throw const FormatException('Academic term dates are invalid.');
    }
    return TimetableAcademicTerm(
      academicYear: _requiredString(json, 'academicYear'),
      code: _requiredString(json, 'code'),
      name: _requiredString(json, 'name'),
      startsOn: startsOn,
      endsOn: endsOn,
    );
  }

  final String academicYear;
  final String code;
  final String name;
  final DateTime startsOn;
  final DateTime endsOn;
}

enum TimetableDayOfWeek {
  monday('MONDAY', 'Thứ hai', 'T2'),
  tuesday('TUESDAY', 'Thứ ba', 'T3'),
  wednesday('WEDNESDAY', 'Thứ tư', 'T4'),
  thursday('THURSDAY', 'Thứ năm', 'T5'),
  friday('FRIDAY', 'Thứ sáu', 'T6'),
  saturday('SATURDAY', 'Thứ bảy', 'T7'),
  sunday('SUNDAY', 'Chủ nhật', 'CN');

  const TimetableDayOfWeek(this.apiValue, this.label, this.shortLabel);

  final String apiValue;
  final String label;
  final String shortLabel;

  static TimetableDayOfWeek fromJson(String value) {
    for (final dayOfWeek in values) {
      if (dayOfWeek.apiValue == value) return dayOfWeek;
    }
    throw FormatException('Invalid dayOfWeek: $value.');
  }
}

final class TimetableDay {
  const TimetableDay({
    required this.date,
    required this.dayOfWeek,
    required this.lessons,
  });

  factory TimetableDay.fromJson(Map<String, dynamic> json) {
    final rawLessons = _requiredList(json, 'lessons');
    return TimetableDay(
      date: _requiredIsoDate(json, 'date'),
      dayOfWeek: TimetableDayOfWeek.fromJson(
        _requiredString(json, 'dayOfWeek'),
      ),
      lessons: rawLessons
          .map((rawLesson) {
            if (rawLesson is! Map) {
              throw const FormatException('Invalid timetable lesson.');
            }
            return TimetableLesson.fromJson(
              Map<String, dynamic>.from(rawLesson),
            );
          })
          .toList(growable: false),
    );
  }

  final DateTime date;
  final TimetableDayOfWeek dayOfWeek;
  final List<TimetableLesson> lessons;
}

enum TimetableSession {
  morning('MORNING', 'Buổi sáng'),
  afternoon('AFTERNOON', 'Buổi chiều');

  const TimetableSession(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static TimetableSession fromJson(String value) {
    for (final session in values) {
      if (session.apiValue == value) return session;
    }
    throw FormatException('Invalid lesson session: $value.');
  }
}

enum TimetableLessonStatus {
  scheduled('SCHEDULED', 'Theo lịch'),
  cancelled('CANCELLED', 'Đã hủy'),
  replaced('REPLACED', 'Thay thế'),
  added('ADDED', 'Học bù');

  const TimetableLessonStatus(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static TimetableLessonStatus fromJson(String value) {
    for (final status in values) {
      if (status.apiValue == value) return status;
    }
    throw FormatException('Invalid lesson status: $value.');
  }
}

final class TimetableSubject {
  const TimetableSubject({required this.code, required this.name});

  factory TimetableSubject.fromJson(Map<String, dynamic> json) {
    return TimetableSubject(
      code: _requiredString(json, 'code'),
      name: _requiredString(json, 'name'),
    );
  }

  final String code;
  final String name;
}

final class TimetableLesson {
  const TimetableLesson({
    required this.session,
    required this.periodNumber,
    required this.startTime,
    required this.endTime,
    required this.subject,
    required this.teacherName,
    required this.room,
    required this.status,
    required this.note,
  });

  factory TimetableLesson.fromJson(Map<String, dynamic> json) {
    final startTime = _requiredLocalTime(json, 'startTime');
    final endTime = _requiredLocalTime(json, 'endTime');
    if (_minutesSinceMidnight(endTime) - _minutesSinceMidnight(startTime) !=
        45) {
      throw const FormatException('A lesson must be exactly 45 minutes.');
    }
    return TimetableLesson(
      session: TimetableSession.fromJson(_requiredString(json, 'session')),
      periodNumber: _requiredPeriodNumber(json, 'periodNumber'),
      startTime: startTime,
      endTime: endTime,
      subject: TimetableSubject.fromJson(_requiredMap(json, 'subject')),
      teacherName: _requiredNullableString(json, 'teacherName'),
      room: _requiredNullableString(json, 'room'),
      status: TimetableLessonStatus.fromJson(_requiredString(json, 'status')),
      note: _requiredNullableString(json, 'note'),
    );
  }

  final TimetableSession session;
  final int periodNumber;
  final String startTime;
  final String endTime;
  final TimetableSubject subject;
  final String? teacherName;
  final String? room;
  final TimetableLessonStatus status;
  final String? note;
}

String formatTimetableDate(DateTime date) {
  String twoDigits(int value) => value.toString().padLeft(2, '0');
  return '${date.year.toString().padLeft(4, '0')}-${twoDigits(date.month)}-'
      '${twoDigits(date.day)}';
}

Map<String, dynamic> _requiredMap(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! Map) {
    throw FormatException('Missing required object: $key.');
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
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Missing required string: $key.');
  }
  return value.trim();
}

String? _requiredNullableString(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Invalid nullable string: $key.');
  }
  return value.trim();
}

int _requiredPeriodNumber(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value < 1 || value > 5) {
    throw FormatException('Invalid period number: $key.');
  }
  return value;
}

DateTime _requiredIsoDate(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(value);
  if (match == null) {
    throw FormatException('Invalid date: $key.');
  }
  final date = DateTime.utc(
    int.parse(match.group(1)!),
    int.parse(match.group(2)!),
    int.parse(match.group(3)!),
  );
  if (formatTimetableDate(date) != value) {
    throw FormatException('Invalid date: $key.');
  }
  return date;
}

String _requiredLocalTime(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  if (!RegExp(r'^([01]\d|2[0-3]):[0-5]\d$').hasMatch(value)) {
    throw FormatException('Invalid local time: $key.');
  }
  return value;
}

int _minutesSinceMidnight(String value) {
  final parts = value.split(':');
  return int.parse(parts.first) * 60 + int.parse(parts.last);
}

bool _isSameDate(DateTime left, DateTime right) {
  return left.year == right.year &&
      left.month == right.month &&
      left.day == right.day;
}

bool _overlapsWeek(
  TimetableAcademicTerm term,
  DateTime weekStart,
  DateTime weekEnd,
) {
  return !term.endsOn.isBefore(weekStart) && !term.startsOn.isAfter(weekEnd);
}

int _compareAcademicTerms(
  TimetableAcademicTerm left,
  TimetableAcademicTerm right,
) {
  final startComparison = left.startsOn.compareTo(right.startsOn);
  if (startComparison != 0) return startComparison;
  final endComparison = left.endsOn.compareTo(right.endsOn);
  if (endComparison != 0) return endComparison;
  return left.code.compareTo(right.code);
}

void _validateLessonCoverage(
  List<TimetableDay> days,
  List<TimetableAcademicTerm> academicTerms,
) {
  for (final day in days) {
    if (day.lessons.isEmpty) continue;
    if (academicTerms.isEmpty ||
        !academicTerms.any((term) => _coversDate(term, day.date))) {
      throw const FormatException(
        'Each timetable lesson must be covered by an academic term.',
      );
    }
  }
}

bool _coversDate(TimetableAcademicTerm term, DateTime date) {
  return !date.isBefore(term.startsOn) && !date.isAfter(term.endsOn);
}
