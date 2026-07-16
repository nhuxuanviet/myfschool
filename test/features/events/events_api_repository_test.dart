import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/src/core/network/api_client.dart';
import 'package:myfschoolse1913/src/features/events/data/events_api_repository.dart';
import 'package:myfschoolse1913/src/features/events/domain/school_event.dart';

import '../../helpers/test_events.dart';

void main() {
  test(
    'loads the upcoming events feed without optional query parameters',
    () async {
      late RequestOptions requestedOptions;
      final repository = ApiEventsRepository(
        ApiClient(
          _dioReturning(testEventsFeedJson(), (options) {
            requestedOptions = options;
          }),
        ),
      );

      final feed = await repository.getEvents();

      expect(requestedOptions.path, ApiEventsRepository.eventsPath);
      expect(requestedOptions.queryParameters, isEmpty);
      expect(feed.events, hasLength(3));
    },
  );

  test('sends category and includePast filters only when requested', () async {
    late RequestOptions requestedOptions;
    final repository = ApiEventsRepository(
      ApiClient(
        _dioReturning(testEventsFeedJson(), (options) {
          requestedOptions = options;
        }),
      ),
    );

    await repository.getEvents(
      category: EventCategory.sports,
      includePast: true,
    );

    expect(
      requestedOptions.queryParameters,
      equals(const {'category': 'SPORTS', 'includePast': true}),
    );
  });

  test(
    'uses detail and registration endpoints with their returned event',
    () async {
      final requests = <RequestOptions>[];
      final registeredEvent = testSchoolEventJson(
        registrationStatus: 'REGISTERED',
        canRegister: false,
        canCancel: true,
      );
      final cancelledEvent = testSchoolEventJson(
        registrationStatus: 'CANCELLED',
        canRegister: false,
        canCancel: false,
      );
      final responses = [
        testSchoolEventJson(),
        registeredEvent,
        cancelledEvent,
      ];
      var responseIndex = 0;
      final repository = ApiEventsRepository(
        ApiClient(
          _dioReturningDynamic((options) {
            requests.add(options);
            return responses[responseIndex++];
          }),
        ),
      );

      final detail = await repository.getEvent(
        testAcademicEventId.toUpperCase(),
      );
      final registered = await repository.register(testAcademicEventId);
      final cancelled = await repository.cancelRegistration(
        testAcademicEventId,
      );

      expect(detail.id, testAcademicEventId);
      expect(registered.registrationStatus, EventRegistrationStatus.registered);
      expect(cancelled.registrationStatus, EventRegistrationStatus.cancelled);
      expect(requests.map((request) => request.method), [
        'GET',
        'POST',
        'DELETE',
      ]);
      expect(requests.map((request) => request.path), [
        '/api/v1/events/$testAcademicEventId',
        '/api/v1/events/$testAcademicEventId/registrations',
        '/api/v1/events/$testAcademicEventId/registrations',
      ]);
    },
  );
}

Dio _dioReturning(
  Map<String, dynamic> responseJson,
  void Function(RequestOptions options) onRequest,
) {
  return _dioReturningDynamic((options) {
    onRequest(options);
    return responseJson;
  });
}

Dio _dioReturningDynamic(
  Map<String, dynamic> Function(RequestOptions options) responseForRequest,
) {
  return Dio()
    ..interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          handler.resolve(
            Response<Object?>(
              requestOptions: options,
              statusCode: 200,
              data: responseForRequest(options),
            ),
          );
        },
      ),
    );
}
