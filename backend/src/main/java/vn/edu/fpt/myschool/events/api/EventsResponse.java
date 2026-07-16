package vn.edu.fpt.myschool.events.api;

import java.util.List;

import vn.edu.fpt.myschool.events.domain.EventDetails;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

public record EventsResponse(String timeZone, List<EventResponse> events) {

    static EventsResponse from(List<EventDetails> events) {
        return new EventsResponse(
                SchoolTimeZone.ZONE.getId(),
                events.stream().map(EventResponse::from).toList());
    }
}
