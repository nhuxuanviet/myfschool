const _clubUtcOffset = Duration(hours: 7);
final _clubUuidPattern = RegExp(
  r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
);
final _clubInstantPattern = RegExp(
  r'^(\d{4})-(\d{2})-(\d{2})T([01]\d|2[0-3]):([0-5]\d):([0-5]\d)(?:\.\d{1,9})?(?:Z|[+-](?:[01]\d|2[0-3]):[0-5]\d)$',
);

enum ClubCategory {
  academic('ACADEMIC', 'Học thuật'),
  sports('SPORTS', 'Thể thao'),
  arts('ARTS', 'Nghệ thuật'),
  skills('SKILLS', 'Kỹ năng'),
  community('COMMUNITY', 'Cộng đồng'),
  media('MEDIA', 'Truyền thông');

  const ClubCategory(this.apiValue, this.label);
  final String apiValue;
  final String label;

  static ClubCategory fromJson(String value) => values.firstWhere(
    (category) => category.apiValue == value,
    orElse: () => throw FormatException('Invalid club category: $value.'),
  );
}

enum ClubMembershipStatus {
  notApplied('NOT_APPLIED', 'Chưa đăng ký'),
  pending('PENDING', 'Chờ duyệt'),
  active('ACTIVE', 'Đang tham gia'),
  rejected('REJECTED', 'Từ chối'),
  withdrawn('WITHDRAWN', 'Đã rút đơn');

  const ClubMembershipStatus(this.apiValue, this.label);
  final String apiValue;
  final String label;

  static ClubMembershipStatus fromJson(String value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () =>
        throw FormatException('Invalid club membership status: $value.'),
  );
}

final class ClubsFeed {
  const ClubsFeed({required this.clubs});

  factory ClubsFeed.fromJson(Map<String, dynamic> json) {
    final value = json['clubs'];
    if (value is! List) {
      throw const FormatException('Missing clubs list.');
    }
    final clubs = value
        .map((item) {
          if (item is! Map) {
            throw const FormatException('Invalid club item.');
          }
          return SchoolClub.fromJson(Map<String, dynamic>.from(item));
        })
        .toList(growable: false);
    final ids = <String>{};
    for (final club in clubs) {
      if (!ids.add(club.id)) {
        throw const FormatException('Club IDs must be unique.');
      }
    }
    return ClubsFeed(clubs: clubs);
  }

  final List<SchoolClub> clubs;
}

final class SchoolClub {
  const SchoolClub({
    required this.id,
    required this.category,
    required this.name,
    required this.description,
    required this.advisorName,
    required this.meetingSchedule,
    required this.location,
    required this.audienceGradeLevel,
    required this.capacity,
    required this.activeMemberCount,
    required this.applicationDeadline,
    required this.membershipStatus,
    required this.canApply,
    required this.canWithdraw,
  });

  factory SchoolClub.fromJson(Map<String, dynamic> json) {
    final capacity = _nullablePositiveInt(json, 'capacity');
    final count = _nonNegativeInt(json, 'activeMemberCount');
    if (capacity != null && count > capacity) {
      throw const FormatException('Club active membership exceeds capacity.');
    }
    final status = ClubMembershipStatus.fromJson(
      _string(json, 'membershipStatus'),
    );
    final canApply = _bool(json, 'canApply');
    final canWithdraw = _bool(json, 'canWithdraw');
    if (canApply && canWithdraw ||
        canWithdraw != (status == ClubMembershipStatus.pending)) {
      throw const FormatException('Invalid club membership actions.');
    }
    if ((status == ClubMembershipStatus.pending ||
            status == ClubMembershipStatus.active) &&
        canApply) {
      throw const FormatException('Existing memberships cannot apply again.');
    }
    return SchoolClub(
      id: _uuid(json, 'id'),
      category: ClubCategory.fromJson(_string(json, 'category')),
      name: _string(json, 'name'),
      description: _string(json, 'description'),
      advisorName: _string(json, 'advisorName'),
      meetingSchedule: _string(json, 'meetingSchedule'),
      location: _string(json, 'location'),
      audienceGradeLevel: _nullableGrade(json, 'audienceGradeLevel'),
      capacity: capacity,
      activeMemberCount: count,
      applicationDeadline: _nullableInstant(json, 'applicationDeadline'),
      membershipStatus: status,
      canApply: canApply,
      canWithdraw: canWithdraw,
    );
  }

  final String id;
  final ClubCategory category;
  final String name;
  final String description;
  final String advisorName;
  final String meetingSchedule;
  final String location;
  final int? audienceGradeLevel;
  final int? capacity;
  final int activeMemberCount;
  final DateTime? applicationDeadline;
  final ClubMembershipStatus membershipStatus;
  final bool canApply;
  final bool canWithdraw;
}

String? canonicalClubId(String? value) {
  if (value == null || !_clubUuidPattern.hasMatch(value)) {
    return null;
  }
  return value.toLowerCase();
}

String formatClubInstant(DateTime instant) {
  final value = instant.toUtc().add(_clubUtcOffset);
  return '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')} • '
      '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
}

String _string(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! String || value.trim().isEmpty) {
    throw FormatException('Missing string: $key.');
  }
  return value.trim();
}

String _uuid(Map<String, dynamic> json, String key) {
  final value = canonicalClubId(_string(json, key));
  if (value == null) {
    throw FormatException('Invalid club ID: $key.');
  }
  return value;
}

bool _bool(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! bool) {
    throw FormatException('Missing boolean: $key.');
  }
  return value;
}

int _nonNegativeInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is! int || value < 0) {
    throw FormatException('Invalid count: $key.');
  }
  return value;
}

int? _nullablePositiveInt(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) {
    return null;
  }
  if (value is! int || value <= 0) {
    throw FormatException('Invalid positive integer: $key.');
  }
  return value;
}

int? _nullableGrade(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) {
    return null;
  }
  if (value is! int || value < 6 || value > 12) {
    throw FormatException('Invalid grade: $key.');
  }
  return value;
}

DateTime? _nullableInstant(Map<String, dynamic> json, String key) {
  if (!json.containsKey(key)) {
    throw FormatException('Missing nullable field: $key.');
  }
  final value = json[key];
  if (value == null) {
    return null;
  }
  if (value is! String) {
    throw FormatException('Invalid instant: $key.');
  }
  final match = _clubInstantPattern.firstMatch(value);
  if (match == null) {
    throw FormatException('Invalid instant: $key.');
  }
  final year = int.parse(match.group(1)!);
  final month = int.parse(match.group(2)!);
  final day = int.parse(match.group(3)!);
  final calendarDate = DateTime.utc(year, month, day);
  if (calendarDate.year != year ||
      calendarDate.month != month ||
      calendarDate.day != day) {
    throw FormatException('Invalid instant: $key.');
  }
  final parsed = DateTime.tryParse(value);
  if (parsed == null) {
    throw FormatException('Invalid instant: $key.');
  }
  return parsed.toUtc();
}
