package vn.edu.fpt.myschool.timetable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class TimetableIntegrationTest {

    private static final UUID SEEDED_ACADEMIC_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID MATHEMATICS_SUBJECT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID PHYSICS_SUBJECT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000604");
    private static final String SEEDED_CLASS_NAME = "10A1";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    void requiresAuthenticationAndAllowsOnlyStudentGetRequests() throws Exception {
        mockMvc.perform(get("/api/v1/timetable"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/timetable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsASevenDayThptTimetableWithSeededExceptionsAndFortyFiveMinutePeriods()
            throws Exception {
        String response = mockMvc.perform(get("/api/v1/timetable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.academicTerms.length()").value(1))
                .andExpect(jsonPath("$.academicTerms[0].code").value("HK1"))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.days[6].dayOfWeek").value("SUNDAY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LocalDate expectedWeekStart = schoolToday().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(LocalDate.parse(JsonPath.read(response, "$.weekStart")))
                .isEqualTo(expectedWeekStart);
        assertThat(LocalDate.parse(JsonPath.read(response, "$.weekEnd")))
                .isEqualTo(expectedWeekStart.plusDays(6));

        List<Map<String, Object>> days = JsonPath.read(response, "$.days");
        assertThat(days).hasSize(7);
        for (int index = 0; index < days.size(); index++) {
            LocalDate date = LocalDate.parse((String) days.get(index).get("date"));
            assertThat(date).isEqualTo(expectedWeekStart.plusDays(index));
            assertThat(days.get(index).get("dayOfWeek")).isEqualTo(date.getDayOfWeek().name());
        }

        List<Map<String, Object>> lessons = JsonPath.read(response, "$.days[*].lessons[*]");
        assertThat(lessons).isNotEmpty();
        assertThat(lessons.stream().map(lesson -> (String) lesson.get("status")))
                .contains("SCHEDULED", "CANCELLED", "REPLACED", "ADDED");
        assertThat(lessons.stream()
                .filter(lesson -> !"SCHEDULED".equals(lesson.get("status")))
                .map(lesson -> (String) lesson.get("note")))
                .allSatisfy(note -> assertThat(note).isNotBlank());

        for (Map<String, Object> lesson : lessons) {
            String startTime = (String) lesson.get("startTime");
            String endTime = (String) lesson.get("endTime");
            assertThat(startTime).matches("\\d{2}:\\d{2}");
            assertThat(endTime).matches("\\d{2}:\\d{2}");
            assertThat(Duration.between(LocalTime.parse(startTime), LocalTime.parse(endTime)))
                    .isEqualTo(Duration.ofMinutes(45));
        }

        List<Map<String, Object>> wednesdayLessons = lessonsFor(days, 2);
        assertThat(wednesdayLessons).isSortedAccordingTo(Comparator
                .comparingInt((Map<String, Object> lesson) ->
                        "MORNING".equals(lesson.get("session")) ? 0 : 1)
                .thenComparing(lesson -> (String) lesson.get("startTime"))
                .thenComparing(lesson -> ((Number) lesson.get("periodNumber")).intValue()));
    }

    @Test
    void derivesTheClassFromTheJwtSubjectInsteadOfClientSuppliedStudentIdentifiers() throws Exception {
        mockMvc.perform(get("/api/v1/timetable")
                        .param("studentId", UUID.randomUUID().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].lessons[0].subject.code").value("TOAN"));
    }

    @Test
    void validatesWeekStartAndUsesAnExplicitMondayVerbatim() throws Exception {
        LocalDate requestedWeekStart = schoolToday()
                .plusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        mockMvc.perform(get("/api/v1/timetable")
                        .param("weekStart", requestedWeekStart.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(requestedWeekStart.toString()))
                .andExpect(jsonPath("$.weekEnd").value(requestedWeekStart.plusDays(6).toString()));

        mockMvc.perform(get("/api/v1/timetable")
                        .param("weekStart", requestedWeekStart.plusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }

    @Test
    void returnsAnEmptyAcademicTermsListWhenTheWeekDoesNotOverlapATerm() throws Exception {
        LocalDate weekStart = LocalDate.of(2099, 1, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        mockMvc.perform(get("/api/v1/timetable")
                        .param("weekStart", weekStart.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicTerms").isArray())
                .andExpect(jsonPath("$.academicTerms.length()").value(0))
                .andExpect(jsonPath("$.days.length()").value(7));
    }

    @Test
    void returnsEveryTermAndItsLessonsWhenAWeekCrossesAnAcademicTermBoundary() throws Exception {
        LocalDate weekStart = LocalDate.of(2034, 7, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        UUID yearId = UUID.randomUUID();
        UUID firstTermId = UUID.randomUUID();
        UUID secondTermId = UUID.randomUUID();
        Instant now = clock.instant();
        jdbcTemplate.update("""
                INSERT INTO academic_years (id, code, starts_on, ends_on, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                yearId,
                "2034-2035",
                weekStart.minusDays(1),
                weekStart.plusDays(8),
                Timestamp.from(now),
                Timestamp.from(now));
        insertAcademicTerm(
                firstTermId,
                yearId,
                "TERM_A",
                "Term A",
                weekStart,
                weekStart.plusDays(2),
                now);
        insertAcademicTerm(
                secondTermId,
                yearId,
                "TERM_B",
                "Term B",
                weekStart.plusDays(3),
                weekStart.plusDays(6),
                now);
        insertPeriodDefinition(firstTermId, now);
        insertPeriodDefinition(secondTermId, now);
        insertRecurringLesson(
                firstTermId,
                DayOfWeek.MONDAY,
                MATHEMATICS_SUBJECT_ID,
                "Teacher A",
                now);
        insertRecurringLesson(
                secondTermId,
                DayOfWeek.THURSDAY,
                PHYSICS_SUBJECT_ID,
                "Teacher B",
                now);

        mockMvc.perform(get("/api/v1/timetable")
                        .param("weekStart", weekStart.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicTerms.length()").value(2))
                .andExpect(jsonPath("$.academicTerms[0].code").value("TERM_A"))
                .andExpect(jsonPath("$.academicTerms[1].code").value("TERM_B"))
                .andExpect(jsonPath("$.days[0].lessons[0].subject.code").value("TOAN"))
                .andExpect(jsonPath("$.days[3].lessons[0].subject.code").value("VAT_LI"));
    }

    @Test
    void enforcesTheExactFortyFiveMinuteDurationInPostgreSql() {
        UUID academicYearId = UUID.randomUUID();
        UUID academicTermId = UUID.randomUUID();
        Instant now = clock.instant();
        LocalDate startsOn = schoolToday();
        jdbcTemplate.update("""
                INSERT INTO academic_years (id, code, starts_on, ends_on, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                academicYearId,
                "DURATION-TEST",
                startsOn.minusDays(1),
                startsOn.plusDays(10),
                Timestamp.from(now),
                Timestamp.from(now));
        jdbcTemplate.update("""
                INSERT INTO academic_terms (
                    id, academic_year_id, code, name, starts_on, ends_on, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                academicTermId,
                academicYearId,
                "DURATION",
                "Duration test term",
                startsOn,
                startsOn.plusDays(7),
                Timestamp.from(now),
                Timestamp.from(now));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO term_period_definitions (
                    id, academic_term_id, session, period_number, start_time, end_time,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                academicTermId,
                "MORNING",
                1,
                Time.valueOf(LocalTime.of(7, 0)),
                Time.valueOf(LocalTime.of(7, 40)),
                Timestamp.from(now),
                Timestamp.from(now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsOverlappingAcademicTermsWithinTheSameAcademicYear() {
        UUID academicYearId = UUID.randomUUID();
        LocalDate startsOn = LocalDate.of(2040, 1, 1);
        Instant now = clock.instant();
        jdbcTemplate.update("""
                INSERT INTO academic_years (id, code, starts_on, ends_on, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                academicYearId,
                "2040-2041",
                startsOn,
                startsOn.plusDays(30),
                Timestamp.from(now),
                Timestamp.from(now));
        insertAcademicTerm(
                UUID.randomUUID(),
                academicYearId,
                "OVERLAP_A",
                "Overlap A",
                startsOn,
                startsOn.plusDays(7),
                now);

        assertThatThrownBy(() -> insertAcademicTerm(
                UUID.randomUUID(),
                academicYearId,
                "OVERLAP_B",
                "Overlap B",
                startsOn.plusDays(7),
                startsOn.plusDays(14),
                now))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnAddedOverrideForAnExistingRecurringSlot() {
        assertThatThrownBy(() -> insertOverride(
                schoolWeekStart().plusDays(1),
                "MORNING",
                2,
                "ADDED",
                PHYSICS_SUBJECT_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ADDED overrides require an empty recurring timetable slot");
    }

    @Test
    void rejectsACancelledOverrideForAnEmptyRecurringSlot() {
        assertThatThrownBy(() -> insertOverride(
                schoolWeekStart().plusDays(5),
                "MORNING",
                2,
                "CANCELLED",
                null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(
                        "CANCELLED and REPLACED overrides require a recurring timetable entry");
    }

    @Test
    void rejectsAReplacedOverrideForAnEmptyRecurringSlot() {
        assertThatThrownBy(() -> insertOverride(
                schoolWeekStart().plusDays(6),
                "MORNING",
                1,
                "REPLACED",
                PHYSICS_SUBJECT_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(
                        "CANCELLED and REPLACED overrides require a recurring timetable entry");
    }

    @Test
    void rejectsAnOverrideDateOutsideItsAcademicTerm() {
        assertThatThrownBy(() -> insertOverride(
                schoolWeekStart().plusYears(1),
                "MORNING",
                1,
                "ADDED",
                PHYSICS_SUBJECT_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Timetable override date must fall within its academic term");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lessonsFor(List<Map<String, Object>> days, int dayIndex) {
        return (List<Map<String, Object>>) days.get(dayIndex).get("lessons");
    }

    private LocalDate schoolToday() {
        return LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE);
    }

    private LocalDate schoolWeekStart() {
        return schoolToday().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void insertAcademicTerm(
            UUID termId,
            UUID academicYearId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO academic_terms (
                    id, academic_year_id, code, name, starts_on, ends_on, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                termId,
                academicYearId,
                code,
                name,
                startsOn,
                endsOn,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertPeriodDefinition(UUID academicTermId, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO term_period_definitions (
                    id, academic_term_id, session, period_number, start_time, end_time,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                academicTermId,
                "MORNING",
                1,
                Time.valueOf(LocalTime.of(7, 0)),
                Time.valueOf(LocalTime.of(7, 45)),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertRecurringLesson(
            UUID academicTermId,
            DayOfWeek dayOfWeek,
            UUID subjectId,
            String teacherName,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO class_timetable_entries (
                    id, academic_term_id, class_name, day_of_week, session, period_number,
                    subject_id, teacher_name, room, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                academicTermId,
                SEEDED_CLASS_NAME,
                dayOfWeek.getValue(),
                "MORNING",
                1,
                subjectId,
                teacherName,
                "P.201",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private int insertOverride(
            LocalDate lessonDate,
            String session,
            int periodNumber,
            String overrideType,
            UUID subjectId) {
        Instant now = clock.instant();
        return jdbcTemplate.update("""
                INSERT INTO timetable_overrides (
                    id, academic_term_id, class_name, lesson_date, session, period_number,
                    override_type, subject_id, teacher_name, room, note, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                SEEDED_ACADEMIC_TERM_ID,
                SEEDED_CLASS_NAME,
                lessonDate,
                session,
                periodNumber,
                overrideType,
                subjectId,
                null,
                null,
                "Override validation test.",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private String loginAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber":"0912345678","password":"Student@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
