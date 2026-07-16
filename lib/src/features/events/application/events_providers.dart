import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/data/auth_network_providers.dart';
import '../data/events_api_repository.dart';
import '../domain/events_repository.dart';
import '../domain/school_event.dart';

final eventsRepositoryProvider = Provider<EventsRepository>(
  (ref) => ApiEventsRepository(ref.watch(authenticatedApiClientProvider)),
);

final class EventsFilter {
  const EventsFilter({this.category, this.includePast = false});

  final EventCategory? category;
  final bool includePast;

  EventsFilter copyWith({
    EventCategory? category,
    bool clearCategory = false,
    bool? includePast,
  }) {
    return EventsFilter(
      category: clearCategory ? null : category ?? this.category,
      includePast: includePast ?? this.includePast,
    );
  }
}

final eventsFilterProvider =
    NotifierProvider.autoDispose<_EventsFilterNotifier, EventsFilter>(
      _EventsFilterNotifier.new,
    );

final class _EventsFilterNotifier extends Notifier<EventsFilter> {
  @override
  EventsFilter build() => const EventsFilter();

  void selectCategory(EventCategory? category) {
    state = category == null
        ? state.copyWith(clearCategory: true)
        : state.copyWith(category: category);
  }

  void setIncludePast(bool includePast) {
    state = state.copyWith(includePast: includePast);
  }
}

final eventsFeedProvider = FutureProvider.autoDispose<EventsFeed>((ref) {
  final filter = ref.watch(eventsFilterProvider);
  return ref
      .watch(eventsRepositoryProvider)
      .getEvents(category: filter.category, includePast: filter.includePast);
});

/// Loads one server-owned event for a restored or shared details URL.
final eventDetailsProvider = FutureProvider.autoDispose
    .family<SchoolEvent, String>((ref, eventId) async {
      final canonicalEventId = canonicalSchoolEventId(eventId);
      if (canonicalEventId == null) {
        throw const FormatException(
          'The requested event identifier is invalid.',
        );
      }
      final event = await ref
          .watch(eventsRepositoryProvider)
          .getEvent(canonicalEventId);
      if (event.id != canonicalEventId) {
        throw const FormatException(
          'The event details response did not match the requested event.',
        );
      }
      return event;
    });

final class EventRegistrationState {
  const EventRegistrationState({
    this.activeEventId,
    this.isSubmitting = false,
    this.errorMessage,
  });

  final String? activeEventId;
  final bool isSubmitting;
  final String? errorMessage;
}

final eventRegistrationControllerProvider =
    NotifierProvider.autoDispose<
      EventRegistrationController,
      EventRegistrationState
    >(EventRegistrationController.new);

/// Serializes registration mutations and invalidates stale event snapshots
/// only after the authoritative server response has arrived.
class EventRegistrationController extends Notifier<EventRegistrationState> {
  @override
  EventRegistrationState build() => const EventRegistrationState();

  Future<bool> register(String eventId) {
    return _submit(eventId, ref.read(eventsRepositoryProvider).register);
  }

  Future<bool> cancel(String eventId) {
    return _submit(
      eventId,
      ref.read(eventsRepositoryProvider).cancelRegistration,
    );
  }

  Future<bool> _submit(
    String eventId,
    Future<SchoolEvent> Function(String eventId) action,
  ) async {
    if (state.isSubmitting) return false;
    final canonicalEventId = canonicalSchoolEventId(eventId);
    if (canonicalEventId == null) {
      state = EventRegistrationState(
        activeEventId: eventId,
        errorMessage: 'Không thể cập nhật đăng ký sự kiện. Vui lòng thử lại.',
      );
      return false;
    }
    state = EventRegistrationState(
      activeEventId: canonicalEventId,
      isSubmitting: true,
    );
    try {
      final updatedEvent = await action(canonicalEventId);
      if (!ref.mounted) return false;
      if (updatedEvent.id != canonicalEventId) {
        throw const FormatException(
          'The event registration response did not match the requested event.',
        );
      }
      state = const EventRegistrationState();
      ref.invalidate(eventDetailsProvider(canonicalEventId));
      ref.invalidate(eventsFeedProvider);
      return true;
    } on Object {
      if (!ref.mounted) return false;
      state = EventRegistrationState(
        activeEventId: canonicalEventId,
        errorMessage: 'Không thể cập nhật đăng ký sự kiện. Vui lòng thử lại.',
      );
      ref.invalidate(eventDetailsProvider(canonicalEventId));
      ref.invalidate(eventsFeedProvider);
      return false;
    }
  }
}
