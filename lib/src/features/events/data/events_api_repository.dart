import '../../../core/network/api_client.dart';
import '../domain/events_repository.dart';
import '../domain/school_event.dart';

final class ApiEventsRepository implements EventsRepository {
  const ApiEventsRepository(this._apiClient);

  static const eventsPath = '/api/v1/events';

  final ApiClient _apiClient;

  @override
  Future<EventsFeed> getEvents({
    EventCategory? category,
    bool includePast = false,
  }) async {
    final queryParameters = <String, dynamic>{
      if (category != null) 'category': category.apiValue,
      if (includePast) 'includePast': true,
    };
    final eventsJson = await _apiClient.getJson(
      eventsPath,
      queryParameters: queryParameters.isEmpty ? null : queryParameters,
    );
    return EventsFeed.fromJson(eventsJson);
  }

  @override
  Future<SchoolEvent> getEvent(String eventId) async {
    final eventJson = await _apiClient.getJson(_eventPath(eventId));
    return SchoolEvent.fromJson(eventJson);
  }

  @override
  Future<SchoolEvent> register(String eventId) async {
    final eventJson = await _apiClient.postJson(_registrationPath(eventId));
    return SchoolEvent.fromJson(eventJson);
  }

  @override
  Future<SchoolEvent> cancelRegistration(String eventId) async {
    final eventJson = await _apiClient.deleteJson(_registrationPath(eventId));
    return SchoolEvent.fromJson(eventJson);
  }

  static String _eventPath(String eventId) {
    final canonicalEventId = canonicalSchoolEventId(eventId);
    if (canonicalEventId == null) {
      throw ArgumentError.value(eventId, 'eventId', 'must be a UUID');
    }
    return '$eventsPath/${Uri.encodeComponent(canonicalEventId)}';
  }

  static String _registrationPath(String eventId) {
    return '${_eventPath(eventId)}/registrations';
  }
}
