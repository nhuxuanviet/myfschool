const formsSchoolTimeZone = 'Asia/Ho_Chi_Minh';
const _formsSchoolUtcOffset = Duration(hours: 7);
final _uuidPattern = RegExp(
  r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
);
final _isoInstantPattern = RegExp(
  r'^(\d{4})-(\d{2})-(\d{2})T([01]\d|2[0-3]):([0-5]\d):([0-5]\d)(?:\.\d{1,9})?(?:Z|[+-](?:[01]\d|2[0-3]):[0-5]\d)$',
);
final _isoDatePattern = RegExp(r'^\d{4}-\d{2}-\d{2}$');

enum StudentFormType {
  leaveOfAbsence('LEAVE_OF_ABSENCE', 'Đơn xin nghỉ học'),
  studentConfirmation('STUDENT_CONFIRMATION', 'Giấy xác nhận học sinh'),
  transcriptRequest('TRANSCRIPT_REQUEST', 'Yêu cầu bảng điểm'),
  studentCardReissue('STUDENT_CARD_REISSUE', 'Cấp lại thẻ học sinh');

  const StudentFormType(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static StudentFormType fromJson(String value) {
    return values.firstWhere(
      (type) => type.apiValue == value,
      orElse: () => throw FormatException('Invalid student form type: $value.'),
    );
  }
}

enum StudentFormStatus {
  submitted('SUBMITTED', 'Đã gửi'),
  inReview('IN_REVIEW', 'Đang xử lý'),
  approved('APPROVED', 'Đã duyệt'),
  rejected('REJECTED', 'Từ chối'),
  cancelled('CANCELLED', 'Đã hủy');

  const StudentFormStatus(this.apiValue, this.label);

  final String apiValue;
  final String label;

  bool get isStudentCancellable =>
      this == StudentFormStatus.submitted || this == StudentFormStatus.inReview;

  static StudentFormStatus fromJson(String value) {
    return values.firstWhere(
      (status) => status.apiValue == value,
      orElse: () =>
          throw FormatException('Invalid student form status: $value.'),
    );
  }
}

final class StudentFormsFeed {
  const StudentFormsFeed({required this.forms});

  factory StudentFormsFeed.fromJson(Map<String, dynamic> json) {
    final forms = _requiredList(json, 'forms')
        .map((value) {
          if (value is! Map) {
            throw const FormatException('Invalid student form item.');
          }
          return StudentFormSummary.fromJson(Map<String, dynamic>.from(value));
        })
        .toList(growable: false);
    final ids = <String>{};
    for (final form in forms) {
      if (!ids.add(form.id)) {
        throw const FormatException('Student form IDs must be unique.');
      }
    }
    return StudentFormsFeed(forms: forms);
  }

  final List<StudentFormSummary> forms;
}

final class StudentFormSummary {
  const StudentFormSummary({
    required this.id,
    required this.type,
    required this.startsOn,
    required this.endsOn,
    required this.status,
    required this.submittedAt,
    required this.updatedAt,
    required this.canCancel,
  });

  factory StudentFormSummary.fromJson(Map<String, dynamic> json) {
    final type = StudentFormType.fromJson(_requiredString(json, 'type'));
    final startsOn = _requiredNullableDate(json, 'startsOn');
    final endsOn = _requiredNullableDate(json, 'endsOn');
    _validateDates(type, startsOn, endsOn);
    final status = StudentFormStatus.fromJson(_requiredString(json, 'status'));
    final canCancel = _requiredBool(json, 'canCancel');
    if (canCancel != status.isStudentCancellable) {
      throw const FormatException('Invalid student form cancellation action.');
    }
    final submittedAt = _requiredInstant(json, 'submittedAt');
    final updatedAt = _requiredInstant(json, 'updatedAt');
    if (updatedAt.isBefore(submittedAt)) {
      throw const FormatException('Student form update precedes submission.');
    }
    return StudentFormSummary(
      id: _requiredUuid(json, 'id'),
      type: type,
      startsOn: startsOn,
      endsOn: endsOn,
      status: status,
      submittedAt: submittedAt,
      updatedAt: updatedAt,
      canCancel: canCancel,
    );
  }

  final String id;
  final StudentFormType type;
  final DateTime? startsOn;
  final DateTime? endsOn;
  final StudentFormStatus status;
  final DateTime submittedAt;
  final DateTime updatedAt;
  final bool canCancel;
}

final class StudentFormDetails {
  const StudentFormDetails({
    required this.summary,
    required this.reason,
    required this.timeline,
  });

  factory StudentFormDetails.fromJson(Map<String, dynamic> json) {
    final summary = StudentFormSummary.fromJson(json);
    final timeline = _requiredList(json, 'timeline')
        .map((value) {
          if (value is! Map) {
            throw const FormatException('Invalid student form timeline item.');
          }
          return StudentFormTimelineEntry.fromJson(
            Map<String, dynamic>.from(value),
          );
        })
        .toList(growable: false);
    if (timeline.isEmpty || timeline.last.status != summary.status) {
      throw const FormatException('Student form timeline is incomplete.');
    }
    for (var index = 1; index < timeline.length; index += 1) {
      if (timeline[index].occurredAt.isBefore(timeline[index - 1].occurredAt)) {
        throw const FormatException(
          'Student form timeline is not chronological.',
        );
      }
    }
    return StudentFormDetails(
      summary: summary,
      reason: _requiredString(json, 'reason'),
      timeline: timeline,
    );
  }

  final StudentFormSummary summary;
  final String reason;
  final List<StudentFormTimelineEntry> timeline;
}

final class StudentFormTimelineEntry {
  const StudentFormTimelineEntry({
    required this.id,
    required this.status,
    required this.occurredAt,
    required this.note,
  });

  factory StudentFormTimelineEntry.fromJson(Map<String, dynamic> json) {
    return StudentFormTimelineEntry(
      id: _requiredUuid(json, 'id'),
      status: StudentFormStatus.fromJson(_requiredString(json, 'status')),
      occurredAt: _requiredInstant(json, 'occurredAt'),
      note: _nullableNonBlankString(json, 'note'),
    );
  }

  final String id;
  final StudentFormStatus status;
  final DateTime occurredAt;
  final String? note;
}

String? canonicalStudentFormId(String? value) {
  if (value == null || !_uuidPattern.hasMatch(value)) return null;
  return value.toLowerCase();
}

String formatStudentFormDate(DateTime date) {
  return '${_twoDigits(date.day)}/${_twoDigits(date.month)}/${date.year}';
}

String formatStudentFormInstant(DateTime instant) {
  final schoolTime = instant.toUtc().add(_formsSchoolUtcOffset);
  return '${_twoDigits(schoolTime.hour)}:${_twoDigits(schoolTime.minute)} • '
      '${formatStudentFormDate(schoolTime)}';
}

String formatStudentFormDateRange(DateTime startsOn, DateTime endsOn) {
  if (startsOn == endsOn) return formatStudentFormDate(startsOn);
  return '${formatStudentFormDate(startsOn)} – ${formatStudentFormDate(endsOn)}';
}

String formatIsoStudentFormDate(DateTime date) {
  return '${date.year.toString().padLeft(4, '0')}-'
      '${_twoDigits(date.month)}-${_twoDigits(date.day)}';
}

DateTime? tryParseStudentFormDate(String value) {
  final trimmed = value.trim();
  if (!_isoDatePattern.hasMatch(trimmed)) return null;
  final parts = trimmed.split('-').map(int.parse).toList(growable: false);
  final parsed = DateTime.utc(parts[0], parts[1], parts[2]);
  if (parsed.year != parts[0] ||
      parsed.month != parts[1] ||
      parsed.day != parts[2]) {
    return null;
  }
  return parsed;
}

void _validateDates(
  StudentFormType type,
  DateTime? startsOn,
  DateTime? endsOn,
) {
  if (type == StudentFormType.leaveOfAbsence) {
    if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
      throw const FormatException('Leave form date range is invalid.');
    }
  } else if (startsOn != null || endsOn != null) {
    throw const FormatException('Only leave forms may contain dates.');
  }
}

List<Object?> _requiredList(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! List) throw FormatException('Missing required list: $key.');
  return List<Object?>.from(value);
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Missing required string: $key.');
  }
  return value.trim();
}

String? _nullableNonBlankString(Map<String, dynamic> json, String key) {
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

bool _requiredBool(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! bool) throw FormatException('Missing required boolean: $key.');
  return value;
}

String _requiredUuid(Map<String, dynamic> json, String key) {
  final canonical = canonicalStudentFormId(_requiredString(json, key));
  if (canonical == null) throw FormatException('Invalid UUID: $key.');
  return canonical;
}

DateTime? _requiredNullableDate(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) return null;
  if (value is! String) throw FormatException('Invalid date: $key.');
  final parsed = tryParseStudentFormDate(value);
  if (parsed == null) throw FormatException('Invalid date: $key.');
  return parsed;
}

DateTime _requiredInstant(Map<String, dynamic> json, String key) {
  final value = _requiredString(json, key);
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
  final local = DateTime.utc(year, month, day, hour, minute, second);
  if (local.year != year ||
      local.month != month ||
      local.day != day ||
      local.hour != hour ||
      local.minute != minute ||
      local.second != second) {
    throw FormatException('Invalid ISO-8601 instant: $key.');
  }
  final parsed = DateTime.tryParse(value);
  if (parsed == null) throw FormatException('Invalid ISO-8601 instant: $key.');
  return parsed.toUtc();
}

String _twoDigits(int value) => value.toString().padLeft(2, '0');
