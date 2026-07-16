/// Events are displayed in the school's fixed timezone, not the device zone.
/// Vietnam does not observe daylight saving time, so UTC+07:00 is stable for
/// the API timezone [eventsSchoolTimeZone].
const eventsSchoolTimeZone = 'Asia/Ho_Chi_Minh';
const eventsSchoolTimeZoneLabel = 'Giờ Việt Nam (UTC+07)';
const _eventsSchoolUtcOffset = Duration(hours: 7);
final _isoInstantPattern = RegExp(
  r'^(\d{4})-(\d{2})-(\d{2})T([01]\d|2[0-3]):([0-5]\d):([0-5]\d)(?:\.\d{1,9})?(?:Z|[+-](?:[01]\d|2[0-3]):[0-5]\d)$',
);

final class EventsFeed {
  const EventsFeed({required this.timeZone, required this.events});

  factory EventsFeed.fromJson(Map<String, dynamic> json) {
    final timeZone = _requiredString(json, 'timeZone');
    if (timeZone != eventsSchoolTimeZone) {
      throw FormatException('Unsupported event timezone: $timeZone.');
    }
    final events = _requiredList(json, 'events')
        .map((value) {
          if (value is! Map) {
            throw const FormatException('Invalid event item.');
          }
          return SchoolEvent.fromJson(Map<String, dynamic>.from(value));
        })
        .toList(growable: false);
    _validateUniqueEventIds(events);

    return EventsFeed(timeZone: timeZone, events: events);
  }

  final String timeZone;
  final List<SchoolEvent> events;
}

enum EventCategory {
  academic('ACADEMIC', 'Học tập'),
  cultural('CULTURAL', 'Văn hóa'),
  sports('SPORTS', 'Thể thao'),
  club('CLUB', 'Câu lạc bộ'),
  career('CAREER', 'Hướng nghiệp');

  const EventCategory(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static EventCategory fromJson(String value) {
    for (final category in values) {
      if (category.apiValue == value) return category;
    }
    throw FormatException('Invalid event category: $value.');
  }
}

enum EventRegistrationStatus {
  notRegistered('NOT_REGISTERED', 'Chưa đăng ký'),
  registered('REGISTERED', 'Đã đăng ký'),
  cancelled('CANCELLED', 'Đã hủy');

  const EventRegistrationStatus(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static EventRegistrationStatus fromJson(String value) {
    for (final status in values) {
      if (status.apiValue == value) return status;
    }
    throw FormatException('Invalid event registration status: $value.');
  }
}

final class SchoolEvent {
  const SchoolEvent({
    required this.id,
    required this.category,
    required this.title,
    required this.description,
    required this.location,
    required this.startsAt,
    required this.endsAt,
    required this.audienceGradeLevel,
    required this.capacity,
    required this.registeredCount,
    required this.registrationDeadline,
    required this.cancellationDeadline,
    required this.registrationStatus,
    required this.canRegister,
    required this.canCancel,
  });

  factory SchoolEvent.fromJson(Map<String, dynamic> json) {
    final startsAt = _requiredIsoInstant(json, 'startsAt');
    final endsAt = _requiredIsoInstant(json, 'endsAt');
    if (!endsAt.isAfter(startsAt)) {
      throw const FormatException('Event end time must be after start time.');
    }
    final registrationDeadline = _requiredNullableIsoInstant(
      json,
      'registrationDeadline',
    );
    final cancellationDeadline = _requiredNullableIsoInstant(
      json,
      'cancellationDeadline',
    );
    _validateDeadlines(
      startsAt: startsAt,
      registrationDeadline: registrationDeadline,
      cancellationDeadline: cancellationDeadline,
    );
    final capacity = _requiredNullablePositiveInt(json, 'capacity');
    final registeredCount = _requiredNonNegativeInt(json, 'registeredCount');
    if (capacity != null && registeredCount > capacity) {
      throw const FormatException('Event registrations exceed capacity.');
    }
    final registrationStatus = EventRegistrationStatus.fromJson(
      _requiredString(json, 'registrationStatus'),
    );
    final canRegister = _requiredBool(json, 'canRegister');
    final canCancel = _requiredBool(json, 'canCancel');
    _validateRegistrationActions(
      status: registrationStatus,
      canRegister: canRegister,
      canCancel: canCancel,
    );

    return SchoolEvent(
      id: _requiredEventId(json, 'id'),
      category: EventCategory.fromJson(_requiredString(json, 'category')),
      title: _requiredString(json, 'title'),
      description: _requiredString(json, 'description'),
      location: _requiredString(json, 'location'),
      startsAt: startsAt,
      endsAt: endsAt,
      audienceGradeLevel: _requiredNullableGradeLevel(
        json,
        'audienceGradeLevel',
      ),
      capacity: capacity,
      registeredCount: registeredCount,
      registrationDeadline: registrationDeadline,
      cancellationDeadline: cancellationDeadline,
      registrationStatus: registrationStatus,
      canRegister: canRegister,
      canCancel: canCancel,
    );
  }

  final String id;
  final EventCategory category;
  final String title;
  final String description;
  final String location;
  final DateTime startsAt;
  final DateTime endsAt;
  final int? audienceGradeLevel;
  final int? capacity;
  final int registeredCount;
  final DateTime? registrationDeadline;
  final DateTime? cancellationDeadline;
  final EventRegistrationStatus registrationStatus;
  final bool canRegister;
  final bool canCancel;
}

/// Converts a UTC instant into Vietnam school time without device-local APIs.
DateTime toEventsSchoolTime(DateTime instant) {
  return instant.toUtc().add(_eventsSchoolUtcOffset);
}

String formatEventDateTime(DateTime instant) {
  final schoolTime = toEventsSchoolTime(instant);
  return '${_twoDigits(schoolTime.hour)}:${_twoDigits(schoolTime.minute)} • '
      '${_twoDigits(schoolTime.day)}/${_twoDigits(schoolTime.month)}/'
      '${schoolTime.year}';
}

String formatEventDateRange(DateTime startsAt, DateTime endsAt) {
  final start = toEventsSchoolTime(startsAt);
  final end = toEventsSchoolTime(endsAt);
  if (_isSameSchoolDate(start, end)) {
    return '${_twoDigits(start.hour)}:${_twoDigits(start.minute)} – '
        '${_twoDigits(end.hour)}:${_twoDigits(end.minute)} • '
        '${_twoDigits(start.day)}/${_twoDigits(start.month)}/${start.year}';
  }
  return '${formatEventDateTime(startsAt)} – ${formatEventDateTime(endsAt)}';
}

bool isValidSchoolEventId(String? value) {
  return value != null &&
      RegExp(
        r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
      ).hasMatch(value);
}

/// Returns a canonical UUID for routing, repository paths, and comparisons.
///
/// UUIDs are case-insensitive, whereas JSON serializers commonly emit their
/// lowercase representation.
String? canonicalSchoolEventId(String? value) {
  if (!isValidSchoolEventId(value)) return null;
  return value!.toLowerCase();
}

String _requiredEventId(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
  final canonicalId = canonicalSchoolEventId(value);
  if (canonicalId == null) {
    throw FormatException('Invalid event identifier: $key.');
  }
  return canonicalId;
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

bool _requiredBool(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! bool) {
    throw FormatException('Missing required boolean: $key.');
  }
  return value;
}

int _requiredNonNegativeInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value < 0) {
    throw FormatException('Invalid non-negative integer: $key.');
  }
  return value;
}

int? _requiredNullablePositiveInt(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! int || value <= 0) {
    throw FormatException('Invalid positive integer: $key.');
  }
  return value;
}

int? _requiredNullableGradeLevel(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! int || value < 6 || value > 12) {
    throw FormatException('Invalid audience grade level: $key.');
  }
  return value;
}

DateTime _requiredIsoInstant(Map<String, dynamic> json, String key) {
  return _parseIsoInstant(_requiredString(json, key), key);
}

DateTime? _requiredNullableIsoInstant(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Invalid nullable instant: $key.');
  }
  return _parseIsoInstant(value.trim(), key);
}

DateTime _parseIsoInstant(String value, String key) {
  final match = _isoInstantPattern.firstMatch(value);
  if (match == null) {
    throw FormatException('Invalid ISO-8601 instant: $key.');
  }

  final year = int.parse(match.group(1)!);
  final month = int.parse(match.group(2)!);
  final day = int.parse(match.group(3)!);
  final hour = int.parse(match.group(4)!);
  final minute = int.parse(match.group(5)!);
  final second = int.parse(match.group(6)!);
  final localDateTime = DateTime.utc(year, month, day, hour, minute, second);
  if (localDateTime.year != year ||
      localDateTime.month != month ||
      localDateTime.day != day ||
      localDateTime.hour != hour ||
      localDateTime.minute != minute ||
      localDateTime.second != second) {
    throw FormatException('Invalid ISO-8601 instant: $key.');
  }

  final parsed = DateTime.tryParse(value);
  if (parsed == null) {
    throw FormatException('Invalid ISO-8601 instant: $key.');
  }
  return parsed.toUtc();
}

void _validateRegistrationActions({
  required EventRegistrationStatus status,
  required bool canRegister,
  required bool canCancel,
}) {
  if (canRegister && canCancel) {
    throw const FormatException('An event cannot register and cancel at once.');
  }
  if (status == EventRegistrationStatus.registered && canRegister) {
    throw const FormatException(
      'Registered events cannot be registered again.',
    );
  }
  if (status != EventRegistrationStatus.registered && canCancel) {
    throw const FormatException('Only registered events can be cancelled.');
  }
}

void _validateDeadlines({
  required DateTime startsAt,
  required DateTime? registrationDeadline,
  required DateTime? cancellationDeadline,
}) {
  if (registrationDeadline != null && registrationDeadline.isAfter(startsAt)) {
    throw const FormatException(
      'Registration deadline must not be after the event start time.',
    );
  }
  if (cancellationDeadline != null && cancellationDeadline.isAfter(startsAt)) {
    throw const FormatException(
      'Cancellation deadline must not be after the event start time.',
    );
  }
}

void _validateUniqueEventIds(List<SchoolEvent> events) {
  final eventIds = <String>{};
  for (final event in events) {
    if (!eventIds.add(event.id)) {
      throw const FormatException('Event IDs must be unique.');
    }
  }
}

bool _isSameSchoolDate(DateTime left, DateTime right) {
  return left.year == right.year &&
      left.month == right.month &&
      left.day == right.day;
}

String _twoDigits(int value) => value.toString().padLeft(2, '0');
