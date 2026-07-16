import 'school_event.dart';

abstract interface class EventsRepository {
  /// Omitting [category] and [includePast] uses the student's upcoming feed.
  Future<EventsFeed> getEvents({
    EventCategory? category,
    bool includePast = false,
  });

  Future<SchoolEvent> getEvent(String eventId);

  Future<SchoolEvent> register(String eventId);

  Future<SchoolEvent> cancelRegistration(String eventId);
}
