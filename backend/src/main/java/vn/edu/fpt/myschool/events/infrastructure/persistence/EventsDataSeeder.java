package vn.edu.fpt.myschool.events.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.events.domain.EventAudience;
import vn.edu.fpt.myschool.events.domain.EventCategory;

/** Stable local and E2E examples that exercise every student event state. */
@Component
@Profile("(dev | e2e) & !prod")
@Order(500)
class EventsDataSeeder implements ApplicationRunner {

    static final UUID OPEN_CULTURAL_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001501");
    static final UUID REGISTERED_ACADEMIC_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001502");
    static final UUID FULL_CLOSED_SPORTS_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001503");
    static final UUID GRADE_ELEVEN_CLUB_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001504");
    static final UUID PAST_CAREER_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001505");
    static final UUID EXPIRED_REGISTRATION_ACADEMIC_EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001506");

    private static final UUID SEEDED_STUDENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID REGISTERED_ACADEMIC_REGISTRATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001601");
    private static final UUID FULL_SPORTS_REGISTRATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001602");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    EventsDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Instant now = clock.instant();
        seedOpenCulturalEvent(now);
        seedRegisteredAcademicEvent(now);
        seedFullClosedSportsEvent(now);
        seedGradeElevenClubEvent(now);
        seedPastCareerEvent(now);
        seedExpiredRegistrationEvent(now);
    }

    private void seedOpenCulturalEvent(Instant now) {
        Instant startsAt = now.plus(Duration.ofDays(14));
        insertEvent(
                OPEN_CULTURAL_EVENT_ID,
                EventCategory.CULTURAL,
                "Liên hoan văn nghệ học sinh",
                "Chương trình biểu diễn và giao lưu văn nghệ dành cho học sinh toàn trường.",
                "Hội trường A",
                startsAt,
                startsAt.plus(Duration.ofHours(3)),
                EventAudience.ALL,
                null,
                120,
                now.plus(Duration.ofDays(10)),
                now.plus(Duration.ofDays(12)),
                true,
                now);
    }

    private void seedRegisteredAcademicEvent(Instant now) {
        Instant startsAt = now.plus(Duration.ofDays(7));
        insertEvent(
                REGISTERED_ACADEMIC_EVENT_ID,
                EventCategory.ACADEMIC,
                "Ngày hội tư vấn chọn môn học",
                "Tư vấn lựa chọn môn học và định hướng tổ hợp cho năm học mới.",
                "Phòng đa năng",
                startsAt,
                startsAt.plus(Duration.ofHours(2)),
                EventAudience.ALL,
                null,
                60,
                now.plus(Duration.ofDays(4)),
                now.plus(Duration.ofDays(6)),
                true,
                now);
        insertRegistered(
                REGISTERED_ACADEMIC_REGISTRATION_ID,
                REGISTERED_ACADEMIC_EVENT_ID,
                now.minus(Duration.ofDays(1)));
    }

    private void seedFullClosedSportsEvent(Instant now) {
        Instant startsAt = now.plus(Duration.ofDays(5));
        insertEvent(
                FULL_CLOSED_SPORTS_EVENT_ID,
                EventCategory.SPORTS,
                "Giải chạy vì sức khỏe học đường",
                "Giải chạy phong trào đã đủ số lượng và đóng đăng ký.",
                "Sân vận động trường",
                startsAt,
                startsAt.plus(Duration.ofHours(4)),
                EventAudience.ALL,
                null,
                1,
                now.plus(Duration.ofDays(2)),
                now.minus(Duration.ofMinutes(30)),
                true,
                now);
        insertRegistered(
                FULL_SPORTS_REGISTRATION_ID,
                FULL_CLOSED_SPORTS_EVENT_ID,
                now.minus(Duration.ofDays(2)));
    }

    private void seedGradeElevenClubEvent(Instant now) {
        Instant startsAt = now.plus(Duration.ofDays(9));
        insertEvent(
                GRADE_ELEVEN_CLUB_EVENT_ID,
                EventCategory.CLUB,
                "Tuyển thành viên câu lạc bộ truyền thông khối 11",
                "Buổi giới thiệu và đăng ký dành riêng cho học sinh khối 11.",
                "Phòng C.302",
                startsAt,
                startsAt.plus(Duration.ofHours(2)),
                EventAudience.GRADE,
                11,
                40,
                now.plus(Duration.ofDays(7)),
                now.plus(Duration.ofDays(8)),
                true,
                now);
    }

    private void seedPastCareerEvent(Instant now) {
        Instant startsAt = now.minus(Duration.ofDays(5));
        insertEvent(
                PAST_CAREER_EVENT_ID,
                EventCategory.CAREER,
                "Chuyên đề định hướng nghề nghiệp",
                "Chuyên đề đã diễn ra về lựa chọn ngành nghề và kỹ năng chuẩn bị hồ sơ.",
                "Thư viện trường",
                startsAt,
                startsAt.plus(Duration.ofHours(2)),
                EventAudience.ALL,
                null,
                null,
                now.minus(Duration.ofDays(6)),
                now.minus(Duration.ofDays(6)),
                false,
                now);
    }

    private void seedExpiredRegistrationEvent(Instant now) {
        Instant startsAt = now.plus(Duration.ofDays(12));
        insertEvent(
                EXPIRED_REGISTRATION_ACADEMIC_EVENT_ID,
                EventCategory.ACADEMIC,
                "Chuyên đề ôn tập đã đóng đăng ký",
                "Chuyên đề học thuật còn thời gian diễn ra nhưng đã hết hạn đăng ký.",
                "Phòng B.205",
                startsAt,
                startsAt.plus(Duration.ofHours(2)),
                EventAudience.ALL,
                null,
                50,
                now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofDays(10)),
                true,
                now);
    }

    private void insertEvent(
            UUID id,
            EventCategory category,
            String title,
            String description,
            String location,
            Instant startsAt,
            Instant endsAt,
            EventAudience audience,
            Integer audienceGradeLevel,
            Integer capacity,
            Instant registrationDeadline,
            Instant cancellationDeadline,
            boolean registrationEnabled,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO school_events (
                    id, category, title, description, location, starts_at, ends_at, audience,
                    audience_grade_level, capacity, registration_deadline, cancellation_deadline,
                    registration_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                id,
                category.name(),
                title,
                description,
                location,
                Timestamp.from(startsAt),
                Timestamp.from(endsAt),
                audience.name(),
                audienceGradeLevel,
                capacity,
                registrationDeadline == null ? null : Timestamp.from(registrationDeadline),
                cancellationDeadline == null ? null : Timestamp.from(cancellationDeadline),
                registrationEnabled,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertRegistered(UUID registrationId, UUID eventId, Instant registeredAt) {
        jdbcTemplate.update("""
                INSERT INTO student_event_registrations (
                    id, event_id, student_id, status, registered_at, cancelled_at, created_at, updated_at
                ) VALUES (?, ?, ?, 'REGISTERED', ?, NULL, ?, ?)
                ON CONFLICT (event_id, student_id) DO NOTHING
                """,
                registrationId,
                eventId,
                SEEDED_STUDENT_ID,
                Timestamp.from(registeredAt),
                Timestamp.from(registeredAt),
                Timestamp.from(registeredAt));
    }
}
