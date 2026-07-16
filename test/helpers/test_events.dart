import 'package:myfschoolse1913/src/features/events/domain/school_event.dart';

const testAcademicEventId = '33333333-3333-4333-8333-333333333333';
const testSportsEventId = '44444444-4444-4444-8444-444444444444';
const testCancelledEventId = '55555555-5555-4555-8555-555555555555';

EventsFeed testEventsFeed({List<Map<String, dynamic>>? events}) {
  return EventsFeed.fromJson(testEventsFeedJson(events: events));
}

Map<String, dynamic> testEventsFeedJson({
  String timeZone = eventsSchoolTimeZone,
  List<Map<String, dynamic>>? events,
}) {
  return {
    'timeZone': timeZone,
    'events':
        events ??
        [
          testSchoolEventJson(),
          testSchoolEventJson(
            id: testSportsEventId,
            category: 'SPORTS',
            title: 'Giải bóng đá khối 10',
            description: 'Vòng chung kết giải bóng đá học sinh.',
            location: 'Sân vận động',
            startsAt: '2026-07-24T06:00:00Z',
            endsAt: '2026-07-24T09:00:00Z',
            audienceGradeLevel: null,
            capacity: 60,
            registeredCount: 24,
            registrationStatus: 'REGISTERED',
            canRegister: false,
            canCancel: true,
          ),
          testSchoolEventJson(
            id: testCancelledEventId,
            category: 'CLUB',
            title: 'Ngày hội câu lạc bộ',
            description: 'Trải nghiệm các câu lạc bộ của trường.',
            location: 'Sảnh đa năng',
            startsAt: '2026-06-15T01:00:00Z',
            endsAt: '2026-06-15T04:00:00Z',
            registrationDeadline: '2026-06-14T10:00:00Z',
            cancellationDeadline: '2026-06-14T16:00:00Z',
            registrationStatus: 'CANCELLED',
            canRegister: false,
            canCancel: false,
          ),
        ],
  };
}

SchoolEvent testSchoolEvent({
  String id = testAcademicEventId,
  String category = 'ACADEMIC',
  String title = 'Ngày hội khoa học 2026',
  String description = 'Khám phá các dự án khoa học của học sinh.',
  String location = 'Hội trường A',
  String startsAt = '2026-07-20T01:00:00Z',
  String endsAt = '2026-07-20T03:30:00Z',
  int? audienceGradeLevel = 10,
  int? capacity = 120,
  int registeredCount = 42,
  String? registrationDeadline = '2026-07-19T10:00:00Z',
  String? cancellationDeadline = '2026-07-19T16:00:00Z',
  String registrationStatus = 'NOT_REGISTERED',
  bool canRegister = true,
  bool canCancel = false,
}) {
  return SchoolEvent.fromJson(
    testSchoolEventJson(
      id: id,
      category: category,
      title: title,
      description: description,
      location: location,
      startsAt: startsAt,
      endsAt: endsAt,
      audienceGradeLevel: audienceGradeLevel,
      capacity: capacity,
      registeredCount: registeredCount,
      registrationDeadline: registrationDeadline,
      cancellationDeadline: cancellationDeadline,
      registrationStatus: registrationStatus,
      canRegister: canRegister,
      canCancel: canCancel,
    ),
  );
}

Map<String, dynamic> testSchoolEventJson({
  String id = testAcademicEventId,
  String category = 'ACADEMIC',
  String title = 'Ngày hội khoa học 2026',
  String description = 'Khám phá các dự án khoa học của học sinh.',
  String location = 'Hội trường A',
  String startsAt = '2026-07-20T01:00:00Z',
  String endsAt = '2026-07-20T03:30:00Z',
  int? audienceGradeLevel = 10,
  int? capacity = 120,
  int registeredCount = 42,
  String? registrationDeadline = '2026-07-19T10:00:00Z',
  String? cancellationDeadline = '2026-07-19T16:00:00Z',
  String registrationStatus = 'NOT_REGISTERED',
  bool canRegister = true,
  bool canCancel = false,
}) {
  return {
    'id': id,
    'category': category,
    'title': title,
    'description': description,
    'location': location,
    'startsAt': startsAt,
    'endsAt': endsAt,
    'audienceGradeLevel': audienceGradeLevel,
    'capacity': capacity,
    'registeredCount': registeredCount,
    'registrationDeadline': registrationDeadline,
    'cancellationDeadline': cancellationDeadline,
    'registrationStatus': registrationStatus,
    'canRegister': canRegister,
    'canCancel': canCancel,
  };
}
