package vn.edu.fpt.myschool.events.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.events.application.port.EventsStore;
import vn.edu.fpt.myschool.events.domain.EventCategory;
import vn.edu.fpt.myschool.events.domain.EventDetails;
import vn.edu.fpt.myschool.events.domain.EventProjection;
import vn.edu.fpt.myschool.events.domain.EventRegistration;
import vn.edu.fpt.myschool.events.domain.EventRegistrationResult;
import vn.edu.fpt.myschool.events.domain.EventRegistrationStatus;
import vn.edu.fpt.myschool.events.domain.SchoolEvent;

@Service
public class EventsService {

    private final StudentProfileStore studentProfileStore;
    private final EventsStore eventsStore;
    private final Clock clock;

    public EventsService(
            StudentProfileStore studentProfileStore,
            EventsStore eventsStore,
            Clock clock) {
        this.studentProfileStore = studentProfileStore;
        this.eventsStore = eventsStore;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EventDetails> getEvents(
            String authenticatedUserId,
            EventCategory category,
            boolean includePast) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        Instant viewedAt = clock.instant();
        return eventsStore.findVisibleEvents(
                        student.id(),
                        student.gradeLevel(),
                        category,
                        includePast,
                        viewedAt)
                .stream()
                .map(projection -> toDetails(projection, viewedAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDetails getEvent(String authenticatedUserId, UUID eventId) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        Instant viewedAt = clock.instant();
        EventProjection projection = eventsStore.findVisibleEvent(
                        eventId,
                        student.id(),
                        student.gradeLevel())
                .orElseThrow(EventsException::eventNotFound);
        return toDetails(projection, viewedAt);
    }

    @Transactional
    public EventRegistrationResult register(String authenticatedUserId, UUID eventId) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        SchoolEvent event = eventsStore.lockVisibleEvent(eventId, student.gradeLevel())
                .orElseThrow(EventsException::eventNotFound);
        Instant now = clock.instant();
        EventRegistration existingRegistration = eventsStore
                .findRegistration(event.id(), student.id())
                .orElse(null);
        if (existingRegistration != null
                && existingRegistration.status() == EventRegistrationStatus.REGISTERED) {
            throw EventsException.alreadyRegistered();
        }
        int registeredCount = eventsStore.countRegistered(event.id());
        assertRegistrationOpen(event, registeredCount, now);

        boolean created = existingRegistration == null;
        if (created) {
            eventsStore.createRegistration(UUID.randomUUID(), event.id(), student.id(), now);
        } else {
            eventsStore.reactivateRegistration(event.id(), student.id(), now);
        }
        return new EventRegistrationResult(
                toDetails(new EventProjection(
                        event,
                        registeredCount + 1,
                        EventRegistrationStatus.REGISTERED), now),
                created);
    }

    @Transactional
    public EventDetails cancelRegistration(String authenticatedUserId, UUID eventId) {
        StudentProfile student = authenticatedStudent(authenticatedUserId);
        SchoolEvent event = eventsStore.lockVisibleEvent(eventId, student.gradeLevel())
                .orElseThrow(EventsException::eventNotFound);
        Instant now = clock.instant();
        eventsStore.findRegistration(event.id(), student.id())
                .filter(existing -> existing.status() == EventRegistrationStatus.REGISTERED)
                .orElseThrow(EventsException::activeRegistrationNotFound);
        if (!isCancellationOpen(event, now)) {
            throw EventsException.cancellationClosed();
        }
        eventsStore.cancelRegistration(event.id(), student.id(), now);
        int registeredCount = eventsStore.countRegistered(event.id());
        return toDetails(new EventProjection(
                event,
                registeredCount,
                EventRegistrationStatus.CANCELLED), now);
    }

    private EventDetails toDetails(EventProjection projection, Instant now) {
        SchoolEvent event = projection.event();
        boolean canRegister = projection.registrationStatus() != EventRegistrationStatus.REGISTERED
                && isRegistrationOpen(event, projection.registeredCount(), now);
        boolean canCancel = projection.registrationStatus() == EventRegistrationStatus.REGISTERED
                && isCancellationOpen(event, now);
        return new EventDetails(
                event,
                projection.registeredCount(),
                projection.registrationStatus(),
                canRegister,
                canCancel);
    }

    private void assertRegistrationOpen(SchoolEvent event, int registeredCount, Instant now) {
        if (!event.registrationEnabled()
                || !event.startsAt().isAfter(now)
                || (event.registrationDeadline() != null && now.isAfter(event.registrationDeadline()))) {
            throw EventsException.registrationClosed();
        }
        if (event.capacity() != null && registeredCount >= event.capacity()) {
            throw EventsException.capacityReached();
        }
    }

    private boolean isRegistrationOpen(SchoolEvent event, int registeredCount, Instant now) {
        return event.registrationEnabled()
                && event.startsAt().isAfter(now)
                && (event.registrationDeadline() == null || !now.isAfter(event.registrationDeadline()))
                && (event.capacity() == null || registeredCount < event.capacity());
    }

    private boolean isCancellationOpen(SchoolEvent event, Instant now) {
        if (event.cancellationDeadline() != null) {
            return !now.isAfter(event.cancellationDeadline());
        }
        return now.isBefore(event.startsAt());
    }

    private StudentProfile authenticatedStudent(String authenticatedUserId) {
        UUID userId = parseAuthenticatedUserId(authenticatedUserId);
        return studentProfileStore.findByUserId(userId)
                .orElseThrow(EventsException::studentProfileNotFound);
    }

    private UUID parseAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw EventsException.invalidAuthenticatedSubject();
        }
        try {
            return UUID.fromString(authenticatedUserId);
        } catch (IllegalArgumentException exception) {
            throw EventsException.invalidAuthenticatedSubject();
        }
    }
}
