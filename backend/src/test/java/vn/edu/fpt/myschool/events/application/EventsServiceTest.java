package vn.edu.fpt.myschool.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.events.application.port.EventsStore;
import vn.edu.fpt.myschool.events.domain.EventAudience;
import vn.edu.fpt.myschool.events.domain.EventCategory;
import vn.edu.fpt.myschool.events.domain.EventDetails;
import vn.edu.fpt.myschool.events.domain.EventProjection;
import vn.edu.fpt.myschool.events.domain.EventRegistration;
import vn.edu.fpt.myschool.events.domain.EventRegistrationResult;
import vn.edu.fpt.myschool.events.domain.EventRegistrationStatus;
import vn.edu.fpt.myschool.events.domain.SchoolEvent;

class EventsServiceTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID STUDENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void rejectsRegistrationWhenTheLockedEventCapacityIsReached() {
        InMemoryEventsStore store = new InMemoryEventsStore(event(1, 1, NOW.plusSeconds(3_600)));
        store.registeredCount = 1;
        EventsService service = service(store);

        assertThatThrownBy(() -> service.register(USER_ID.toString(), store.event.id()))
                .isInstanceOf(EventsException.class)
                .satisfies(exception -> assertThat(((EventsException) exception).getCode())
                        .isEqualTo("EVENT_CAPACITY_REACHED"));
        assertThat(store.createdRegistration).isFalse();
        assertThat(store.locked).isTrue();
    }

    @Test
    void reactivatesACancelledRegistrationWhenTheEventIsStillOpen() {
        InMemoryEventsStore store = new InMemoryEventsStore(event(1, 5, NOW.plusSeconds(3_600)));
        store.registration = new EventRegistration(UUID.randomUUID(), EventRegistrationStatus.CANCELLED);
        EventsService service = service(store);

        EventRegistrationResult result = service.register(USER_ID.toString(), store.event.id());

        assertThat(result.created()).isFalse();
        assertThat(store.reactivatedRegistration).isTrue();
        assertThat(result.event().registrationStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
        assertThat(result.event().registeredCount()).isEqualTo(1);
        assertThat(result.event().canCancel()).isTrue();
    }

    @Test
    void rejectsCancellationAfterTheExplicitCancellationDeadline() {
        SchoolEvent event = new SchoolEvent(
                UUID.randomUUID(),
                EventCategory.ACADEMIC,
                "Event",
                "Description",
                "Location",
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(7_200),
                EventAudience.ALL,
                null,
                5,
                NOW.plusSeconds(1_800),
                NOW.minusSeconds(1),
                true);
        InMemoryEventsStore store = new InMemoryEventsStore(event);
        store.registration = new EventRegistration(UUID.randomUUID(), EventRegistrationStatus.REGISTERED);
        store.registeredCount = 1;
        EventsService service = service(store);

        assertThatThrownBy(() -> service.cancelRegistration(USER_ID.toString(), store.event.id()))
                .isInstanceOf(EventsException.class)
                .satisfies(exception -> assertThat(((EventsException) exception).getCode())
                        .isEqualTo("EVENT_CANCELLATION_CLOSED"));
        assertThat(store.cancelledRegistration).isFalse();
    }

    @Test
    void evaluatesTheRegistrationDeadlineAfterObtainingTheEventLock() {
        InMemoryEventsStore store = new InMemoryEventsStore(event(1, 5, NOW.plusSeconds(3_600)));
        MutableClock clock = new MutableClock(NOW);
        store.afterLock = () -> clock.setInstant(NOW.plusSeconds(5_401));
        EventsService service = service(store, clock);

        assertThatThrownBy(() -> service.register(USER_ID.toString(), store.event.id()))
                .isInstanceOf(EventsException.class)
                .satisfies(exception -> assertThat(((EventsException) exception).getCode())
                        .isEqualTo("EVENT_REGISTRATION_CLOSED"));
        assertThat(store.createdRegistration).isFalse();
    }

    @Test
    void evaluatesTheCancellationDeadlineAfterObtainingTheEventLock() {
        InMemoryEventsStore store = new InMemoryEventsStore(event(1, 5, NOW.plusSeconds(3_600)));
        store.registration = new EventRegistration(UUID.randomUUID(), EventRegistrationStatus.REGISTERED);
        store.registeredCount = 1;
        MutableClock clock = new MutableClock(NOW);
        store.afterLock = () -> clock.setInstant(NOW.plusSeconds(3_601));
        EventsService service = service(store, clock);

        assertThatThrownBy(() -> service.cancelRegistration(USER_ID.toString(), store.event.id()))
                .isInstanceOf(EventsException.class)
                .satisfies(exception -> assertThat(((EventsException) exception).getCode())
                        .isEqualTo("EVENT_CANCELLATION_CLOSED"));
        assertThat(store.cancelledRegistration).isFalse();
    }

    @Test
    void exposesTimeSensitiveActionFlagsOnVisibleEvents() {
        InMemoryEventsStore store = new InMemoryEventsStore(event(1, 5, NOW.plusSeconds(3_600)));
        EventsService service = service(store);

        EventDetails details = service.getEvent(USER_ID.toString(), store.event.id());

        assertThat(details.canRegister()).isTrue();
        assertThat(details.canCancel()).isFalse();
    }

    private static EventsService service(InMemoryEventsStore store) {
        return service(store, CLOCK);
    }

    private static EventsService service(InMemoryEventsStore store, Clock clock) {
        StudentProfileStore students = ignored -> Optional.of(new StudentProfile(
                STUDENT_ID,
                "SE1913001",
                "Nguyễn Minh Anh",
                10,
                "10A1"));
        return new EventsService(students, store, clock);
    }

    private static SchoolEvent event(int identifier, Integer capacity, Instant cancellationDeadline) {
        return new SchoolEvent(
                new UUID(0, identifier),
                EventCategory.ACADEMIC,
                "Event " + identifier,
                "Description",
                "Location",
                NOW.plusSeconds(7_200),
                NOW.plusSeconds(10_800),
                EventAudience.ALL,
                null,
                capacity,
                NOW.plusSeconds(5_400),
                cancellationDeadline,
                true);
    }

    private static final class InMemoryEventsStore implements EventsStore {

        private final SchoolEvent event;
        private int registeredCount;
        private EventRegistration registration;
        private boolean locked;
        private boolean createdRegistration;
        private boolean reactivatedRegistration;
        private boolean cancelledRegistration;
        private Runnable afterLock = () -> { };

        private InMemoryEventsStore(SchoolEvent event) {
            this.event = event;
        }

        @Override
        public List<EventProjection> findVisibleEvents(
                UUID studentId,
                int gradeLevel,
                EventCategory category,
                boolean includePast,
                Instant viewedAt) {
            return List.of(projection());
        }

        @Override
        public Optional<EventProjection> findVisibleEvent(UUID eventId, UUID studentId, int gradeLevel) {
            return event.id().equals(eventId) ? Optional.of(projection()) : Optional.empty();
        }

        @Override
        public Optional<SchoolEvent> lockVisibleEvent(UUID eventId, int gradeLevel) {
            locked = true;
            if (!event.id().equals(eventId)) {
                return Optional.empty();
            }
            afterLock.run();
            return Optional.of(event);
        }

        @Override
        public Optional<EventRegistration> findRegistration(UUID eventId, UUID studentId) {
            return Optional.ofNullable(registration);
        }

        @Override
        public int countRegistered(UUID eventId) {
            return registeredCount;
        }

        @Override
        public void createRegistration(UUID registrationId, UUID eventId, UUID studentId, Instant registeredAt) {
            createdRegistration = true;
            registration = new EventRegistration(registrationId, EventRegistrationStatus.REGISTERED);
            registeredCount++;
        }

        @Override
        public void reactivateRegistration(UUID eventId, UUID studentId, Instant registeredAt) {
            reactivatedRegistration = true;
            registration = new EventRegistration(registration.id(), EventRegistrationStatus.REGISTERED);
            registeredCount++;
        }

        @Override
        public void cancelRegistration(UUID eventId, UUID studentId, Instant cancelledAt) {
            cancelledRegistration = true;
            registration = new EventRegistration(registration.id(), EventRegistrationStatus.CANCELLED);
            registeredCount--;
        }

        private EventProjection projection() {
            return new EventProjection(
                    event,
                    registeredCount,
                    registration == null ? EventRegistrationStatus.NOT_REGISTERED : registration.status());
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant) {
            this(instant, ZoneOffset.UTC);
        }

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }
    }
}
