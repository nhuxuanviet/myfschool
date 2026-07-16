package vn.edu.fpt.myschool.events.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.events.domain.EventCategory;
import vn.edu.fpt.myschool.events.domain.EventProjection;
import vn.edu.fpt.myschool.events.domain.EventRegistration;
import vn.edu.fpt.myschool.events.domain.SchoolEvent;

/** Student-scoped event reads and transactional registration persistence. */
public interface EventsStore {

    List<EventProjection> findVisibleEvents(
            UUID studentId,
            int gradeLevel,
            EventCategory category,
            boolean includePast,
            Instant viewedAt);

    Optional<EventProjection> findVisibleEvent(UUID eventId, UUID studentId, int gradeLevel);

    /** Locks the visible event row to serialize capacity-sensitive mutations. */
    Optional<SchoolEvent> lockVisibleEvent(UUID eventId, int gradeLevel);

    Optional<EventRegistration> findRegistration(UUID eventId, UUID studentId);

    int countRegistered(UUID eventId);

    void createRegistration(UUID registrationId, UUID eventId, UUID studentId, Instant registeredAt);

    void reactivateRegistration(UUID eventId, UUID studentId, Instant registeredAt);

    void cancelRegistration(UUID eventId, UUID studentId, Instant cancelledAt);
}
