package vn.edu.fpt.myschool.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.events.application.EventsService;
import vn.edu.fpt.myschool.shared.error.ApiException;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class EventsIntegrationTest {

    private static final String SEEDED_PHONE_NUMBER = "0912345678";
    private static final String PEER_PHONE_NUMBER = "0977777777";
    private static final String PASSWORD = "Student@123";
    private static final UUID SEEDED_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PEER_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID OPEN_CULTURAL_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001501");
    private static final UUID REGISTERED_ACADEMIC_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001502");
    private static final UUID FULL_CLOSED_SPORTS_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001503");
    private static final UUID GRADE_ELEVEN_CLUB_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001504");
    private static final UUID PAST_CAREER_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001505");
    private static final UUID EXPIRED_REGISTRATION_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001506");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventsService eventsService;

    @Autowired
    private Clock clock;

    @Test
    void requiresAuthenticationForAllEventRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(delete("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listsOnlyVisibleCurrentEventsAndSupportsCategoryAndPastFiltering() throws Exception {
        String accessToken = loginAccessToken(SEEDED_PHONE_NUMBER);
        mockMvc.perform(get("/api/v1/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.events.length()").value(4))
                .andExpect(jsonPath("$.events[0].id").value(FULL_CLOSED_SPORTS_EVENT_ID.toString()))
                .andExpect(jsonPath("$.events[1].id").value(REGISTERED_ACADEMIC_EVENT_ID.toString()))
                .andExpect(jsonPath("$.events[2].id").value(EXPIRED_REGISTRATION_EVENT_ID.toString()))
                .andExpect(jsonPath("$.events[3].id").value(OPEN_CULTURAL_EVENT_ID.toString()))
                .andExpect(jsonPath("$.events[*].id").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(GRADE_ELEVEN_CLUB_EVENT_ID.toString()))))
                .andExpect(jsonPath("$.events[*].id").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(PAST_CAREER_EVENT_ID.toString()))));

        mockMvc.perform(get("/api/v1/events")
                        .param("category", "CULTURAL")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].id").value(OPEN_CULTURAL_EVENT_ID.toString()))
                .andExpect(jsonPath("$.events[0].category").value("CULTURAL"));

        mockMvc.perform(get("/api/v1/events")
                        .param("includePast", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(5))
                .andExpect(jsonPath("$.events[4].id").value(PAST_CAREER_EVENT_ID.toString()));
    }

    @Test
    void hidesGradeScopedEventsFromOtherStudentsWithoutRevealingTheirExistence() throws Exception {
        String peerAccessToken = loginAccessToken(PEER_PHONE_NUMBER);
        String peerResponse = mockMvc.perform(get("/api/v1/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(peerAccessToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> peerVisibleIds = JsonPath.read(peerResponse, "$.events[*].id");
        assertThat(peerVisibleIds).contains(GRADE_ELEVEN_CLUB_EVENT_ID.toString());

        mockMvc.perform(get("/api/v1/events/{eventId}", GRADE_ELEVEN_CLUB_EVENT_ID)
                        .param("studentId", "00000000-0000-0000-0000-000000000999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken(SEEDED_PHONE_NUMBER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("The requested event is unavailable"));
    }

    @Test
    void returnsTheExactEventDetailContractForARegisteredStudent() throws Exception {
        mockMvc.perform(get("/api/v1/events/{eventId}", REGISTERED_ACADEMIC_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken(SEEDED_PHONE_NUMBER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(REGISTERED_ACADEMIC_EVENT_ID.toString()))
                .andExpect(jsonPath("$.category").value("ACADEMIC"))
                .andExpect(jsonPath("$.title").value("Ngày hội tư vấn chọn môn học"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.location").isNotEmpty())
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty())
                .andExpect(jsonPath("$.audienceGradeLevel").value(nullValue()))
                .andExpect(jsonPath("$.capacity").value(60))
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.registrationDeadline").isNotEmpty())
                .andExpect(jsonPath("$.cancellationDeadline").isNotEmpty())
                .andExpect(jsonPath("$.registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.canRegister").value(false))
                .andExpect(jsonPath("$.canCancel").value(true));
    }

    @Test
    void createsCancelsAndReactivatesARegistrationWithEventDetailResponses() throws Exception {
        String accessToken = loginAccessToken(SEEDED_PHONE_NUMBER);

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(OPEN_CULTURAL_EVENT_ID.toString()))
                .andExpect(jsonPath("$.registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.canRegister").value(false))
                .andExpect(jsonPath("$.canCancel").value(true));

        mockMvc.perform(delete("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OPEN_CULTURAL_EVENT_ID.toString()))
                .andExpect(jsonPath("$.registrationStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.registeredCount").value(0))
                .andExpect(jsonPath("$.canRegister").value(true))
                .andExpect(jsonPath("$.canCancel").value(false));

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OPEN_CULTURAL_EVENT_ID.toString()))
                .andExpect(jsonPath("$.registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.registeredCount").value(1));
    }

    @Test
    void returnsAConflictForADuplicateRegistration() throws Exception {
        String accessToken = loginAccessToken(SEEDED_PHONE_NUMBER);
        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_ALREADY_REGISTERED"));
    }

    @Test
    void returnsACapacityConflictWithTheCorrectCode() throws Exception {
        String peerAccessToken = loginAccessToken(PEER_PHONE_NUMBER);
        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", FULL_CLOSED_SPORTS_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(peerAccessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_CAPACITY_REACHED"));
    }

    @Test
    void returnsARegistrationDeadlineConflictWithTheCorrectCode() throws Exception {
        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", EXPIRED_REGISTRATION_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken(SEEDED_PHONE_NUMBER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_REGISTRATION_CLOSED"));
    }

    @Test
    void returnsACancellationDeadlineConflictWithTheCorrectCode() throws Exception {
        mockMvc.perform(delete("/api/v1/events/{eventId}/registrations", FULL_CLOSED_SPORTS_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken(SEEDED_PHONE_NUMBER))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_CANCELLATION_CLOSED"));
    }

    @Test
    void returnsNotFoundWhenTheStudentHasNoActiveRegistrationToCancel() throws Exception {
        mockMvc.perform(delete("/api/v1/events/{eventId}/registrations", OPEN_CULTURAL_EVENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginAccessToken(SEEDED_PHONE_NUMBER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_REGISTRATION_NOT_FOUND"));
    }

    @Test
    void databaseRejectsAnAllAudienceEventWithAGradeTarget() {
        Instant now = clock.instant();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO school_events (
                    id, category, title, description, location, starts_at, ends_at, audience,
                    audience_grade_level, capacity, registration_deadline, cancellation_deadline,
                    registration_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                "ACADEMIC",
                "Invalid audience event",
                "The audience check must reject a grade target for ALL.",
                "Test room",
                Timestamp.from(now.plus(Duration.ofDays(1))),
                Timestamp.from(now.plus(Duration.ofDays(1)).plus(Duration.ofHours(1))),
                "ALL",
                10,
                null,
                null,
                null,
                true,
                Timestamp.from(now),
                Timestamp.from(now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsADuplicateRegistrationForTheSameEventAndStudent() {
        Instant now = clock.instant();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO student_event_registrations (
                    id, event_id, student_id, status, registered_at, cancelled_at, created_at, updated_at
                ) VALUES (?, ?, ?, 'REGISTERED', ?, NULL, ?, ?)
                """,
                UUID.randomUUID(),
                REGISTERED_ACADEMIC_EVENT_ID,
                SEEDED_STUDENT_ID,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void serializesConcurrentRegistrationsSoCapacityCannotBeExceeded() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = clock.instant();
        insertCapacityOneEvent(eventId, now);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<String>> results = new ArrayList<>();
            results.add(executor.submit(registrationAttempt(SEEDED_USER_ID, eventId, ready, start)));
            results.add(executor.submit(registrationAttempt(PEER_USER_ID, eventId, ready, start)));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> outcomes = List.of(
                    results.get(0).get(10, TimeUnit.SECONDS),
                    results.get(1).get(10, TimeUnit.SECONDS));
            assertThat(outcomes).containsExactlyInAnyOrder("REGISTERED", "EVENT_CAPACITY_REACHED");
            Integer registeredCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM student_event_registrations
                    WHERE event_id = ? AND status = 'REGISTERED'
                    """, Integer.class, eventId);
            assertThat(registeredCount).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            jdbcTemplate.update("DELETE FROM student_event_registrations WHERE event_id = ?", eventId);
            jdbcTemplate.update("DELETE FROM school_events WHERE id = ?", eventId);
        }
    }

    private Callable<String> registrationAttempt(
            UUID userId,
            UUID eventId,
            CountDownLatch ready,
            CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent registration did not start in time");
            }
            try {
                eventsService.register(userId.toString(), eventId);
                return "REGISTERED";
            } catch (ApiException exception) {
                return exception.getCode();
            }
        };
    }

    private void insertCapacityOneEvent(UUID eventId, Instant now) {
        Instant startsAt = now.plus(Duration.ofDays(2));
        jdbcTemplate.update("""
                INSERT INTO school_events (
                    id, category, title, description, location, starts_at, ends_at, audience,
                    audience_grade_level, capacity, registration_deadline, cancellation_deadline,
                    registration_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventId,
                "ACADEMIC",
                "Concurrent capacity test",
                "Verifies event row locking under simultaneous registrations.",
                "Test room",
                Timestamp.from(startsAt),
                Timestamp.from(startsAt.plus(Duration.ofHours(1))),
                "ALL",
                null,
                1,
                Timestamp.from(now.plus(Duration.ofDays(1))),
                Timestamp.from(now.plus(Duration.ofDays(1))),
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private String loginAccessToken(String phoneNumber) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"%s","password":"%s"}
                                """.formatted(phoneNumber, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
