package vn.edu.fpt.myschool.timetable.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;
import vn.edu.fpt.myschool.timetable.domain.SchoolSession;

/**
 * Provides a realistic, self-contained THPT timetable for local development
 * and browser E2E tests. Date-specific overrides are refreshed on startup so
 * the current school week always demonstrates timetable exceptions.
 */
@Component
@Profile("(dev | e2e) & !prod")
@Order(300)
class TimetableDataSeeder implements ApplicationRunner {

    private static final UUID SEEDED_ACADEMIC_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final String SEEDED_CLASS_NAME = "10A1";

    private static final UUID MATHEMATICS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID LITERATURE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000602");
    private static final UUID ENGLISH_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000603");
    private static final UUID PHYSICS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000604");
    private static final UUID CHEMISTRY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000605");
    private static final UUID BIOLOGY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000606");
    private static final UUID HISTORY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000607");
    private static final UUID GEOGRAPHY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000608");
    private static final UUID INFORMATICS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000609");
    private static final UUID PHYSICAL_EDUCATION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000610");

    private static final UUID CANCELLED_LESSON_OVERRIDE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID REPLACED_LESSON_OVERRIDE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000902");
    private static final UUID ADDED_LESSON_OVERRIDE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000903");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    TimetableDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Instant now = clock.instant();
        seedSubjects(now);
        seedPeriodDefinitions(now);
        seedRecurringLessons(now);
        seedCurrentWeekOverrides(now);
    }

    private void seedSubjects(Instant now) {
        insertSubject(MATHEMATICS_ID, "TOAN", "Toán", now);
        insertSubject(LITERATURE_ID, "NGU_VAN", "Ngữ văn", now);
        insertSubject(ENGLISH_ID, "TIENG_ANH", "Tiếng Anh", now);
        insertSubject(PHYSICS_ID, "VAT_LI", "Vật lí", now);
        insertSubject(CHEMISTRY_ID, "HOA_HOC", "Hóa học", now);
        insertSubject(BIOLOGY_ID, "SINH_HOC", "Sinh học", now);
        insertSubject(HISTORY_ID, "LICH_SU", "Lịch sử", now);
        insertSubject(GEOGRAPHY_ID, "DIA_LI", "Địa lí", now);
        insertSubject(INFORMATICS_ID, "TIN_HOC", "Tin học", now);
        insertSubject(PHYSICAL_EDUCATION_ID, "GIAO_DUC_THE_CHAT", "Giáo dục thể chất", now);
    }

    private void seedPeriodDefinitions(Instant now) {
        insertPeriod(701, SchoolSession.MORNING, 1, LocalTime.of(7, 0), now);
        insertPeriod(702, SchoolSession.MORNING, 2, LocalTime.of(7, 50), now);
        insertPeriod(703, SchoolSession.MORNING, 3, LocalTime.of(8, 45), now);
        insertPeriod(704, SchoolSession.MORNING, 4, LocalTime.of(9, 35), now);
        insertPeriod(705, SchoolSession.MORNING, 5, LocalTime.of(10, 25), now);
        insertPeriod(706, SchoolSession.AFTERNOON, 1, LocalTime.of(13, 0), now);
        insertPeriod(707, SchoolSession.AFTERNOON, 2, LocalTime.of(13, 50), now);
        insertPeriod(708, SchoolSession.AFTERNOON, 3, LocalTime.of(14, 45), now);
        insertPeriod(709, SchoolSession.AFTERNOON, 4, LocalTime.of(15, 35), now);
        insertPeriod(710, SchoolSession.AFTERNOON, 5, LocalTime.of(16, 25), now);
    }

    private void seedRecurringLessons(Instant now) {
        insertRecurringLesson(801, DayOfWeek.MONDAY, SchoolSession.MORNING, 1, MATHEMATICS_ID,
                "Cô Nguyễn Thu Hà", "P.201", now);
        insertRecurringLesson(802, DayOfWeek.MONDAY, SchoolSession.MORNING, 2, LITERATURE_ID,
                "Cô Trần Ngọc Mai", "P.201", now);
        insertRecurringLesson(803, DayOfWeek.TUESDAY, SchoolSession.MORNING, 1, ENGLISH_ID,
                "Thầy Lê Quang Huy", "P.201", now);
        insertRecurringLesson(804, DayOfWeek.TUESDAY, SchoolSession.MORNING, 2, PHYSICS_ID,
                "Thầy Phạm Minh Đức", "P.202", now);
        insertRecurringLesson(805, DayOfWeek.TUESDAY, SchoolSession.MORNING, 3, CHEMISTRY_ID,
                "Cô Vũ Thanh Lan", "P.203", now);
        insertRecurringLesson(806, DayOfWeek.WEDNESDAY, SchoolSession.MORNING, 1, MATHEMATICS_ID,
                "Cô Nguyễn Thu Hà", "P.201", now);
        insertRecurringLesson(807, DayOfWeek.WEDNESDAY, SchoolSession.MORNING, 2, BIOLOGY_ID,
                "Cô Đặng Minh Phương", "P.301", now);
        insertRecurringLesson(808, DayOfWeek.WEDNESDAY, SchoolSession.AFTERNOON, 1, INFORMATICS_ID,
                "Thầy Đỗ Quốc Khánh", "P. máy tính 1", now);
        insertRecurringLesson(809, DayOfWeek.THURSDAY, SchoolSession.MORNING, 1, HISTORY_ID,
                "Cô Bùi Anh Thư", "P.201", now);
        insertRecurringLesson(810, DayOfWeek.THURSDAY, SchoolSession.MORNING, 2, GEOGRAPHY_ID,
                "Thầy Nguyễn Việt Long", "P.201", now);
        insertRecurringLesson(811, DayOfWeek.FRIDAY, SchoolSession.MORNING, 1, LITERATURE_ID,
                "Cô Trần Ngọc Mai", "P.201", now);
        insertRecurringLesson(812, DayOfWeek.FRIDAY, SchoolSession.MORNING, 2, PHYSICAL_EDUCATION_ID,
                "Thầy Hoàng Đức Anh", "Sân thể chất", now);
    }

    private void seedCurrentWeekOverrides(Instant now) {
        LocalDate schoolToday = LocalDate.ofInstant(now, SchoolTimeZone.ZONE);
        LocalDate weekStart = schoolToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        upsertOverride(
                CANCELLED_LESSON_OVERRIDE_ID,
                weekStart.plusDays(1),
                SchoolSession.MORNING,
                3,
                "CANCELLED",
                null,
                null,
                null,
                "Nghỉ học theo kế hoạch của nhà trường.",
                now);
        upsertOverride(
                REPLACED_LESSON_OVERRIDE_ID,
                weekStart.plusDays(2),
                SchoolSession.MORNING,
                2,
                "REPLACED",
                INFORMATICS_ID,
                "Thầy Đỗ Quốc Khánh",
                "P. máy tính 1",
                "Đổi tiết theo kế hoạch chuyên môn.",
                now);
        upsertOverride(
                ADDED_LESSON_OVERRIDE_ID,
                weekStart.plusDays(5),
                SchoolSession.MORNING,
                1,
                "ADDED",
                PHYSICS_ID,
                "Thầy Phạm Minh Đức",
                "P.202",
                "Học bù Vật lí theo kế hoạch của nhà trường.",
                now);
    }

    private void insertSubject(UUID id, String code, String name, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO subjects (id, code, name, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, id, code, name, Timestamp.from(now), Timestamp.from(now));
    }

    private void insertPeriod(
            int identifier,
            SchoolSession session,
            int periodNumber,
            LocalTime startTime,
            Instant now) {
        UUID id = seededIdentifier(identifier);
        jdbcTemplate.update("""
                INSERT INTO term_period_definitions (
                    id, academic_term_id, session, period_number, start_time, end_time,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
                id,
                SEEDED_ACADEMIC_TERM_ID,
                session.name(),
                periodNumber,
                startTime,
                startTime.plusMinutes(45),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertRecurringLesson(
            int identifier,
            DayOfWeek dayOfWeek,
            SchoolSession session,
            int periodNumber,
            UUID subjectId,
            String teacherName,
            String room,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO class_timetable_entries (
                    id, academic_term_id, class_name, day_of_week, session, period_number,
                    subject_id, teacher_id, room, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """,
                seededIdentifier(identifier),
                SEEDED_ACADEMIC_TERM_ID,
                SEEDED_CLASS_NAME,
                dayOfWeek.getValue(),
                session.name(),
                periodNumber,
                subjectId,
                teacherId(teacherName, now),
                room,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    /**
     * Resolves a seeded teacher name to a profile, creating one on first sight.
     *
     * <p>The id is derived from the name with the same rule V24 uses, so a database migrated
     * from older data and a freshly seeded one end up pointing at the same profile.
     */
    private UUID teacherId(String teacherName, Instant now) {
        if (teacherName == null || teacherName.isBlank()) {
            return null;
        }
        String fullName = teacherName.strip();
        UUID id = UUID.nameUUIDFromBytes(
                ("timetable-teacher:" + fullName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        jdbcTemplate.update("""
                INSERT INTO teacher_profiles (
                    id, user_id, teacher_code, full_name, enabled, version, created_at, updated_at
                ) VALUES (?, NULL, ?, ?, TRUE, 0, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                id,
                "TKB" + Integer.toHexString(fullName.hashCode()),
                fullName,
                Timestamp.from(now),
                Timestamp.from(now));
        return id;
    }

    private void upsertOverride(
            UUID id,
            LocalDate lessonDate,
            SchoolSession session,
            int periodNumber,
            String overrideType,
            UUID subjectId,
            String teacherName,
            String room,
            String note,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO timetable_overrides (
                    id, academic_term_id, class_name, lesson_date, session, period_number,
                    override_type, subject_id, teacher_id, room, note, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET lesson_date = EXCLUDED.lesson_date,
                    session = EXCLUDED.session,
                    period_number = EXCLUDED.period_number,
                    override_type = EXCLUDED.override_type,
                    subject_id = EXCLUDED.subject_id,
                    teacher_id = EXCLUDED.teacher_id,
                    room = EXCLUDED.room,
                    note = EXCLUDED.note,
                    updated_at = EXCLUDED.updated_at
                """,
                id,
                SEEDED_ACADEMIC_TERM_ID,
                SEEDED_CLASS_NAME,
                lessonDate,
                session.name(),
                periodNumber,
                overrideType,
                subjectId,
                teacherId(teacherName, now),
                room,
                note,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private static UUID seededIdentifier(int identifier) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(identifier));
    }
}
