import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:myfschoolse1913/src/app/app.dart';
import 'package:myfschoolse1913/src/app/app_router.dart';
import 'package:myfschoolse1913/src/features/events/application/events_providers.dart';
import 'package:myfschoolse1913/src/features/events/domain/events_repository.dart';
import 'package:myfschoolse1913/src/features/events/domain/school_event.dart';
import 'package:myfschoolse1913/src/features/events/presentation/event_details_page.dart';
import 'package:myfschoolse1913/src/features/events/presentation/events_page.dart';

import '../../helpers/test_events.dart';

void main() {
  testWidgets('filters events through stable accessible controls', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeEventsRepository(initialFeed: testEventsFeed());
    final router = await _pumpEvents(tester, repository);
    addTearDown(router.dispose);

    expect(find.byType(EventsPage), findsOneWidget);
    expect(find.bySemanticsLabel('Sự kiện'), findsWidgets);
    expect(find.bySemanticsLabel('Lọc sự kiện'), findsOneWidget);
    await tester.tap(find.bySemanticsLabel('Lọc sự kiện'));
    await tester.pumpAndSettle();
    expect(find.text('Bộ lọc sự kiện'), findsOneWidget);
    expect(find.text('Hiển thị sự kiện đã qua'), findsOneWidget);
    await tester.tap(find.text('Hiển thị sự kiện đã qua'));
    await tester.pumpAndSettle();

    await tester.drag(find.byType(Scrollable).last, const Offset(0, -120));
    await tester.pumpAndSettle();
    final sportsFilter = find.bySemanticsLabel('Lọc: Thể thao');
    expect(sportsFilter, findsOneWidget);

    await tester.tap(sportsFilter);
    await tester.pumpAndSettle();

    await tester.tap(find.text('Xem kết quả'));
    await tester.pumpAndSettle();

    await tester.drag(_eventsScrollable(), const Offset(0, -240));
    await tester.pumpAndSettle();
    expect(find.bySemanticsLabel('Múi giờ hiển thị: UTC+07'), findsOneWidget);

    expect(repository.requests, [
      const _EventRequest(),
      const _EventRequest(includePast: true),
      const _EventRequest(category: EventCategory.sports, includePast: true),
    ]);
    semantics.dispose();
  });

  testWidgets('opens event details with a stable deep-link URL', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final repository = _FakeEventsRepository(initialFeed: testEventsFeed());
    final router = await _pumpEvents(tester, repository);
    addTearDown(router.dispose);

    await _openAcademicEvent(tester);

    expect(router.state.uri.path, '/events/$testAcademicEventId');
    expect(find.byType(EventDetailsPage), findsOneWidget);
    expect(
      find.bySemanticsLabel('Chi tiết sự kiện Ngày hội khoa học 2026'),
      findsOneWidget,
    );
    expect(repository.requestedEventIds, [testAcademicEventId]);

    expect(find.byType(NavigationBar), findsNothing);
    semantics.dispose();
  });

  testWidgets('loads an event details deep link by its ID', (tester) async {
    final repository = _FakeEventsRepository(initialFeed: testEventsFeed());
    final router = await _pumpEvents(
      tester,
      repository,
      initialLocation: '/events/$testSportsEventId',
    );
    addTearDown(router.dispose);

    expect(find.byType(EventDetailsPage), findsOneWidget);
    expect(find.text('Giải bóng đá khối 10'), findsOneWidget);
    expect(find.text(eventsSchoolTimeZoneLabel), findsOneWidget);
    expect(repository.requestedEventIds, [testSportsEventId]);
    expect(find.bySemanticsLabel('Đăng ký sự kiện'), findsNothing);
    expect(find.bySemanticsLabel('Hủy đăng ký'), findsOneWidget);
  });

  testWidgets('canonicalizes uppercase event IDs in restored deep links', (
    tester,
  ) async {
    final repository = _FakeEventsRepository(initialFeed: testEventsFeed());
    final router = await _pumpEvents(
      tester,
      repository,
      initialLocation: '/events/${testSportsEventId.toUpperCase()}',
    );
    addTearDown(router.dispose);

    expect(router.state.uri.path, '/events/$testSportsEventId');
    expect(find.byType(EventDetailsPage), findsOneWidget);
    expect(repository.requestedEventIds, [testSportsEventId]);
  });

  testWidgets('returns to events for an invalid details deep link', (
    tester,
  ) async {
    final repository = _FakeEventsRepository(initialFeed: testEventsFeed());
    final router = await _pumpEvents(
      tester,
      repository,
      initialLocation: '/events/not-an-event-id',
    );
    addTearDown(router.dispose);

    expect(find.byType(EventsPage), findsOneWidget);
    expect(find.byType(EventDetailsPage), findsNothing);
    expect(router.state.uri.path, AppRoutes.events);
  });

  testWidgets(
    'disables registration while mutating then refetches server state',
    (tester) async {
      final registeredEvent = testSchoolEvent(
        registrationStatus: 'REGISTERED',
        canRegister: false,
        canCancel: true,
      );
      final pendingRegistration = Completer<SchoolEvent>();
      final repository = _FakeEventsRepository(
        initialFeed: testEventsFeed(),
        registerHandler: (_) => pendingRegistration.future,
      );
      final router = await _pumpEvents(tester, repository);
      addTearDown(router.dispose);

      await _openAcademicEvent(tester);
      expect(find.bySemanticsLabel('Đăng ký sự kiện'), findsOneWidget);

      await _showRegistrationAction(tester);
      await tester.tap(find.bySemanticsLabel('Đăng ký sự kiện'));
      await tester.pump();

      expect(
        find.bySemanticsLabel('Đang xử lý đăng ký sự kiện'),
        findsOneWidget,
      );
      final registerButton = tester.widget<ElevatedButton>(
        find.widgetWithText(ElevatedButton, 'Đăng ký sự kiện'),
      );
      expect(registerButton.onPressed, isNull);

      pendingRegistration.complete(registeredEvent);
      repository.setEvent(registeredEvent);
      await tester.pumpAndSettle();

      expect(repository.registeredEventIds, [testAcademicEventId]);
      expect(find.bySemanticsLabel('Đăng ký sự kiện'), findsNothing);
      expect(find.bySemanticsLabel('Hủy đăng ký'), findsOneWidget);
    },
  );

  testWidgets(
    'shows a retryable registration error without hiding the action',
    (tester) async {
      final repository = _FakeEventsRepository(
        initialFeed: testEventsFeed(),
        registerHandler: (_) =>
            Future<SchoolEvent>.error(StateError('registration unavailable')),
      );
      final router = await _pumpEvents(tester, repository);
      addTearDown(router.dispose);

      await _openAcademicEvent(tester);
      await _showRegistrationAction(tester);
      await tester.tap(find.bySemanticsLabel('Đăng ký sự kiện'));
      await tester.pumpAndSettle();

      expect(
        find.bySemanticsLabel(
          RegExp('Lỗi thao tác sự kiện: Không thể cập nhật'),
        ),
        findsOneWidget,
      );
      final registerButton = tester.widget<ElevatedButton>(
        find.widgetWithText(ElevatedButton, 'Đăng ký sự kiện'),
      );
      expect(registerButton.onPressed, isNotNull);
      expect(repository.requestedEventIds, [
        testAcademicEventId,
        testAcademicEventId,
      ]);
    },
  );

  testWidgets('shows a retry state when the events feed cannot be loaded', (
    tester,
  ) async {
    final pending = Completer<EventsFeed>();
    var requests = 0;
    final repository = _FakeEventsRepository.withFeedHandler((_) {
      requests += 1;
      return requests == 1 ? pending.future : Future.value(testEventsFeed());
    });
    final router = await _pumpEvents(tester, repository, settle: false);
    addTearDown(router.dispose);

    expect(find.bySemanticsLabel('Đang tải sự kiện'), findsOneWidget);
    pending.completeError(StateError('offline'));
    await tester.pump();
    expect(find.bySemanticsLabel('Không thể tải sự kiện'), findsOneWidget);

    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(
      find.bySemanticsLabel('Xem sự kiện Ngày hội khoa học 2026'),
      findsOneWidget,
    );
    expect(requests, 2);
  });
}

Future<void> _openAcademicEvent(WidgetTester tester) async {
  await tester.drag(_eventsScrollable(), const Offset(0, -320));
  await tester.pumpAndSettle();
  final detailsButton = find.bySemanticsLabel(
    'Xem sự kiện Ngày hội khoa học 2026',
  );
  await tester.ensureVisible(detailsButton);
  await tester.pumpAndSettle();
  await tester.tap(detailsButton);
  await tester.pumpAndSettle();
}

Finder _eventsScrollable() {
  return find
      .descendant(
        of: find.byType(EventsPage),
        matching: find.byType(Scrollable),
      )
      .first;
}

Future<void> _showRegistrationAction(WidgetTester tester) async {
  final scrollable = find
      .descendant(
        of: find.byType(EventDetailsPage),
        matching: find.byType(Scrollable),
      )
      .first;
  await tester.drag(scrollable, const Offset(0, -420));
  await tester.pumpAndSettle();
  final action = find.bySemanticsLabel('Đăng ký sự kiện');
  await tester.ensureVisible(action);
  await tester.pumpAndSettle();
}

Future<GoRouter> _pumpEvents(
  WidgetTester tester,
  EventsRepository repository, {
  String initialLocation = AppRoutes.events,
  bool settle = true,
}) async {
  final router = createAppRouter(initialLocation: initialLocation);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [eventsRepositoryProvider.overrideWithValue(repository)],
      child: FptSchoolsApp(router: router),
    ),
  );
  if (settle) await tester.pumpAndSettle();
  return router;
}

final class _EventRequest {
  const _EventRequest({this.category, this.includePast = false});

  final EventCategory? category;
  final bool includePast;

  @override
  bool operator ==(Object other) {
    return other is _EventRequest &&
        other.category == category &&
        other.includePast == includePast;
  }

  @override
  int get hashCode => Object.hash(category, includePast);
}

final class _FakeEventsRepository implements EventsRepository {
  _FakeEventsRepository({required EventsFeed initialFeed, this.registerHandler})
    : _feedHandler = null,
      feed = initialFeed,
      _eventsById = {for (final event in initialFeed.events) event.id: event};

  _FakeEventsRepository.withFeedHandler(
    Future<EventsFeed> Function(_EventRequest request) feedHandler,
  ) : feed = testEventsFeed(),
      registerHandler = null,
      _feedHandler = feedHandler,
      _eventsById = {};

  final EventsFeed feed;
  final Future<SchoolEvent> Function(String eventId)? registerHandler;
  final Future<EventsFeed> Function(_EventRequest request)? _feedHandler;
  final Map<String, SchoolEvent> _eventsById;
  final List<_EventRequest> requests = [];
  final List<String> requestedEventIds = [];
  final List<String> registeredEventIds = [];
  final List<String> cancelledEventIds = [];

  @override
  Future<EventsFeed> getEvents({
    EventCategory? category,
    bool includePast = false,
  }) {
    final request = _EventRequest(category: category, includePast: includePast);
    requests.add(request);
    if (_feedHandler case final handler?) return handler(request);
    return Future.value(feed);
  }

  @override
  Future<SchoolEvent> getEvent(String eventId) {
    requestedEventIds.add(eventId);
    final event = _eventsById[eventId];
    if (event == null) {
      return Future.error(StateError('Missing event $eventId'));
    }
    return Future.value(event);
  }

  @override
  Future<SchoolEvent> register(String eventId) {
    registeredEventIds.add(eventId);
    if (registerHandler case final handler?) return handler(eventId);
    final registeredEvent = testSchoolEvent(
      registrationStatus: 'REGISTERED',
      canRegister: false,
      canCancel: true,
    );
    setEvent(registeredEvent);
    return Future.value(registeredEvent);
  }

  @override
  Future<SchoolEvent> cancelRegistration(String eventId) {
    cancelledEventIds.add(eventId);
    final cancelledEvent = testSchoolEvent(
      registrationStatus: 'CANCELLED',
      canRegister: false,
      canCancel: false,
    );
    setEvent(cancelledEvent);
    return Future.value(cancelledEvent);
  }

  void setEvent(SchoolEvent event) {
    _eventsById[event.id] = event;
  }
}
