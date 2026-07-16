import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/features/events/domain/school_event.dart';

import '../../helpers/test_events.dart';

void main() {
  test('parses the events feed contract and server registration state', () {
    final feed = EventsFeed.fromJson(testEventsFeedJson());

    expect(feed.timeZone, eventsSchoolTimeZone);
    expect(feed.events, hasLength(3));
    expect(feed.events.first.category, EventCategory.academic);
    expect(
      feed.events.first.registrationStatus,
      EventRegistrationStatus.notRegistered,
    );
    expect(
      feed.events[1].registrationStatus,
      EventRegistrationStatus.registered,
    );
    expect(feed.events[1].canCancel, isTrue);
    expect(
      feed.events.last.registrationStatus,
      EventRegistrationStatus.cancelled,
    );
  });

  test('renders ISO instants in UTC+07 without device-local conversion', () {
    final event = testSchoolEvent(
      startsAt: '2026-07-01T18:30:00Z',
      endsAt: '2026-07-01T20:00:00Z',
      registrationDeadline: '2026-07-01T12:00:00Z',
      cancellationDeadline: '2026-07-01T15:00:00Z',
    );

    expect(formatEventDateTime(event.startsAt), '01:30 • 02/07/2026');
    expect(
      formatEventDateRange(event.startsAt, event.endsAt),
      '01:30 – 03:00 • 02/07/2026',
    );
  });

  test('requires the fixed school timezone and offset-bearing instants', () {
    final wrongZone = testEventsFeedJson(timeZone: 'UTC');
    final noOffset = testEventsFeedJson(
      events: [testSchoolEventJson(startsAt: '2026-07-20T01:00:00')],
    );
    final invalidCalendarDay = testEventsFeedJson(
      events: [testSchoolEventJson(startsAt: '2026-02-30T01:00:00Z')],
    );
    final invalidClockTime = testEventsFeedJson(
      events: [testSchoolEventJson(startsAt: '2026-07-20T24:00:00Z')],
    );
    final invalidMinute = testEventsFeedJson(
      events: [testSchoolEventJson(startsAt: '2026-07-20T12:60:00Z')],
    );
    final invalidSecond = testEventsFeedJson(
      events: [testSchoolEventJson(startsAt: '2026-07-20T12:34:60Z')],
    );

    expect(
      () => EventsFeed.fromJson(wrongZone),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => EventsFeed.fromJson(noOffset),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => EventsFeed.fromJson(invalidCalendarDay),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => EventsFeed.fromJson(invalidClockTime),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => EventsFeed.fromJson(invalidMinute),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => EventsFeed.fromJson(invalidSecond),
      throwsA(isA<FormatException>()),
    );
  });

  test(
    'rejects invalid capacity, audience grade, and registration actions',
    () {
      final overCapacity = testEventsFeedJson(
        events: [testSchoolEventJson(capacity: 10, registeredCount: 11)],
      );
      final invalidGrade = testEventsFeedJson(
        events: [testSchoolEventJson(audienceGradeLevel: 5)],
      );
      final conflictingActions = testEventsFeedJson(
        events: [testSchoolEventJson(canRegister: true, canCancel: true)],
      );

      expect(
        () => EventsFeed.fromJson(overCapacity),
        throwsA(isA<FormatException>()),
      );
      expect(
        () => EventsFeed.fromJson(invalidGrade),
        throwsA(isA<FormatException>()),
      );
      expect(
        () => EventsFeed.fromJson(conflictingActions),
        throwsA(isA<FormatException>()),
      );
    },
  );

  test('rejects registration deadlines after an event starts', () {
    final lateRegistrationDeadline = testEventsFeedJson(
      events: [
        testSchoolEventJson(registrationDeadline: '2026-07-20T01:00:01Z'),
      ],
    );
    final lateCancellationDeadline = testEventsFeedJson(
      events: [
        testSchoolEventJson(cancellationDeadline: '2026-07-20T01:00:01Z'),
      ],
    );

    expect(
      () => EventsFeed.fromJson(lateRegistrationDeadline),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => EventsFeed.fromJson(lateCancellationDeadline),
      throwsA(isA<FormatException>()),
    );
  });

  test('recognizes event IDs used by details deep links', () {
    expect(isValidSchoolEventId(testAcademicEventId), isTrue);
    expect(
      canonicalSchoolEventId(testAcademicEventId.toUpperCase()),
      testAcademicEventId,
    );
    expect(
      SchoolEvent.fromJson(
        testSchoolEventJson(id: testAcademicEventId.toUpperCase()),
      ).id,
      testAcademicEventId,
    );
    expect(isValidSchoolEventId('not-an-event-id'), isFalse);
    expect(isValidSchoolEventId(null), isFalse);
  });
}
