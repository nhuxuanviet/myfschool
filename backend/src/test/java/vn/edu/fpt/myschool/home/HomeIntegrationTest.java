package vn.edu.fpt.myschool.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import vn.edu.fpt.myschool.auth.application.port.AccessTokenIssuer;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserRole;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Testcontainers
@ActiveProfiles({"test", "e2e"})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class HomeIntegrationTest {

    private static final String PHONE_NUMBER = "0912345678";
    private static final String PASSWORD = "Student@123";
    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID OTHER_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID WELCOME_ANNOUNCEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID GRADE_TEN_ANNOUNCEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000402");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void requiresAuthenticationAndAllowsOnlyStudentGetRequests() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsTheSeededDashboardContractForTheAuthenticatedStudent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.studentCode").value("SE1913001"))
                .andExpect(jsonPath("$.student.gradeLevel").value(10))
                .andExpect(jsonPath("$.student.className").value("10A1"))
                .andExpect(jsonPath("$.student.id").doesNotExist())
                .andExpect(jsonPath("$.academicTerm.code").value("HK1"))
                .andExpect(jsonPath("$.academicTerm.name").value("Học kỳ I"))
                .andExpect(jsonPath("$.summary.lessons.today").value(0))
                .andExpect(jsonPath("$.summary.events.upcoming").value(4))
                .andExpect(jsonPath("$.summary.forms.pending").value(1))
                .andExpect(jsonPath("$.summary.clubs.active").value(1))
                .andExpect(jsonPath("$.announcements.length()").value(2))
                .andExpect(jsonPath("$.announcements[0].id").value(
                        WELCOME_ANNOUNCEMENT_ID.toString()))
                .andExpect(jsonPath("$.announcements[1].id").value(
                        GRADE_TEN_ANNOUNCEMENT_ID.toString()))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String seededFullName = jdbcTemplate.queryForObject(
                "SELECT full_name FROM students WHERE id = ?", String.class, SEEDED_STUDENT_ID);
        assertThat(JsonPath.<String>read(response, "$.student.fullName"))
                .isEqualTo(seededFullName);

        LocalDate startsOn = LocalDate.parse(
                JsonPath.<String>read(response, "$.academicTerm.startsOn"));
        LocalDate endsOn = LocalDate.parse(
                JsonPath.<String>read(response, "$.academicTerm.endsOn"));
        LocalDate currentDate = LocalDate.ofInstant(clock.instant(), SchoolTimeZone.ZONE);
        assertThat(startsOn).isBeforeOrEqualTo(currentDate);
        assertThat(endsOn).isAfterOrEqualTo(currentDate);
        assertThat(JsonPath.<String>read(response, "$.academicTerm.academicYear"))
                .matches("\\d{4}-\\d{4}");
        assertThat(Instant.parse(
                JsonPath.<String>read(response, "$.announcements[0].publishedAt")))
                .isAfter(Instant.parse(
                        JsonPath.<String>read(response, "$.announcements[1].publishedAt")));
    }

    @Test
    void derivesTheStudentOnlyFromTheJwtSubjectAndIgnoresStudentIdentifiers() throws Exception {
        insertOtherStudent();
        String tokenWithTamperedStudentClaim = accessTokenIssuer.issue(new UserAccount(
                OTHER_USER_ID,
                "0987654321",
                "unused-password-hash",
                UserRole.STUDENT,
                true,
                new StudentProfile(
                        SEEDED_STUDENT_ID,
                        "TAMPERED",
                        "Tampered Profile",
                        10,
                        "10A1")))
                .value();

        mockMvc.perform(get("/api/v1/home")
                        .param("studentId", SEEDED_STUDENT_ID.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithTamperedStudentClaim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.studentCode").value("SE1913002"))
                .andExpect(jsonPath("$.student.fullName").value("Lê Gia Huy"))
                .andExpect(jsonPath("$.student.gradeLevel").value(11))
                .andExpect(jsonPath("$.student.className").value("11A2"));
    }

    @Test
    void returnsANullAcademicTermWhenNoTermIsActive() throws Exception {
        jdbcTemplate.update("DELETE FROM grade_assessments");
        jdbcTemplate.update("DELETE FROM student_term_subjects");
        jdbcTemplate.update("DELETE FROM timetable_overrides");
        jdbcTemplate.update("DELETE FROM class_timetable_entries");
        jdbcTemplate.update("DELETE FROM term_period_definitions");
        jdbcTemplate.update("DELETE FROM academic_terms");

        String response = mockMvc.perform(get("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Object academicTerm = JsonPath.read(response, "$.academicTerm");
        assertThat(academicTerm).isNull();
    }

    @Test
    void returnsOnlyActiveAnnouncementsForTheStudentAudienceInStableOrder() throws Exception {
        jdbcTemplate.update("DELETE FROM announcements");
        Instant now = clock.instant();
        insertAnnouncement(
                "00000000-0000-0000-0000-000000000501",
                "Latest general",
                "Visible to every student.",
                "ALL",
                null,
                now.minus(Duration.ofMinutes(1)),
                now.minus(Duration.ofHours(1)),
                null);
        insertAnnouncement(
                "00000000-0000-0000-0000-000000000502",
                "Grade ten",
                "Visible to grade 10 only.",
                "GRADE",
                10,
                now.minus(Duration.ofMinutes(2)),
                now.minus(Duration.ofHours(1)),
                null);
        insertAnnouncement(
                "00000000-0000-0000-0000-000000000503",
                "Other grade",
                "Not visible to grade 10.",
                "GRADE",
                11,
                now,
                now.minus(Duration.ofHours(1)),
                null);
        insertAnnouncement(
                "00000000-0000-0000-0000-000000000504",
                "Future",
                "Not visible yet.",
                "ALL",
                null,
                now.plus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(1)),
                null);
        insertAnnouncement(
                "00000000-0000-0000-0000-000000000505",
                "Scheduled publish",
                "Visible window started but publication time has not arrived.",
                "ALL",
                null,
                now.plus(Duration.ofHours(1)),
                now.minus(Duration.ofHours(1)),
                null);
        insertAnnouncement(
                "00000000-0000-0000-0000-000000000506",
                "Expired",
                "No longer visible.",
                "ALL",
                null,
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofMinutes(1)));

        String response = mockMvc.perform(get("/api/v1/home")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAccessToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> titles = JsonPath.read(response, "$.announcements[*].title");
        assertThat(titles).containsExactly("Latest general", "Grade ten");
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

    private void insertOtherStudent() {
        Instant now = clock.instant();
        String passwordHash = passwordEncoder.encode("OtherStudent@123");
        jdbcTemplate.update("""
                INSERT INTO users (
                    id, phone_number, password_hash, role, enabled,
                    credentials_updated_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                OTHER_USER_ID,
                "0987654321",
                passwordHash,
                "STUDENT",
                true,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now));
        jdbcTemplate.update("""
                INSERT INTO students (
                    id, user_id, student_code, full_name, grade_level, class_name,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                OTHER_STUDENT_ID,
                OTHER_USER_ID,
                "SE1913002",
                "Lê Gia Huy",
                11,
                "11A2",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertAnnouncement(
            String id,
            String title,
            String body,
            String audience,
            Integer gradeLevel,
            Instant publishedAt,
            Instant visibleFrom,
            Instant visibleUntil) {
        Instant now = clock.instant();
        jdbcTemplate.update("""
                INSERT INTO announcements (
                    id, title, body, audience, audience_grade_level, published_at,
                    visible_from, visible_until, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.fromString(id),
                title,
                body,
                audience,
                gradeLevel,
                Timestamp.from(publishedAt),
                Timestamp.from(visibleFrom),
                visibleUntil == null ? null : Timestamp.from(visibleUntil),
                Timestamp.from(now),
                Timestamp.from(now));
    }
}
