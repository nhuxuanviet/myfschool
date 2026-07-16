package vn.edu.fpt.myschool.events.api;

import java.time.Instant;
import java.util.UUID;

import vn.edu.fpt.myschool.events.domain.EventDetails;
import vn.edu.fpt.myschool.events.domain.SchoolEvent;

/** Common summary and detail shape for a student-visible event. */
public record EventResponse(
        UUID id,
        String category,
        String title,
        String description,
        String location,
        Instant startsAt,
        Instant endsAt,
        Integer audienceGradeLevel,
        Integer capacity,
        int registeredCount,
        Instant registrationDeadline,
        Instant cancellationDeadline,
        String registrationStatus,
        boolean canRegister,
        boolean canCancel) {

    static EventResponse from(EventDetails details) {
        SchoolEvent event = details.event();
        return new EventResponse(
                event.id(),
                event.category().name(),
                event.title(),
                event.description(),
                event.location(),
                event.startsAt(),
                event.endsAt(),
                event.audienceGradeLevel(),
                event.capacity(),
                details.registeredCount(),
                event.registrationDeadline(),
                event.cancellationDeadline(),
                details.registrationStatus().name(),
                details.canRegister(),
                details.canCancel());
    }
}
