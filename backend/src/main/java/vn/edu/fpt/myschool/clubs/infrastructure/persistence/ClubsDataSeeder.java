package vn.edu.fpt.myschool.clubs.infrastructure.persistence;

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

import vn.edu.fpt.myschool.clubs.domain.ClubAudience;
import vn.edu.fpt.myschool.clubs.domain.ClubCategory;
import vn.edu.fpt.myschool.clubs.domain.ClubMembershipStatus;

@Component
@Profile("(dev | e2e) & !prod")
@Order(700)
class ClubsDataSeeder implements ApplicationRunner {
    static final UUID OPEN_ACADEMIC_CLUB_ID = UUID.fromString("00000000-0000-0000-0000-000000001901");
    static final UUID ACTIVE_SPORTS_CLUB_ID = UUID.fromString("00000000-0000-0000-0000-000000001902");
    static final UUID PENDING_MEDIA_CLUB_ID = UUID.fromString("00000000-0000-0000-0000-000000001903");
    static final UUID FULL_COMMUNITY_CLUB_ID = UUID.fromString("00000000-0000-0000-0000-000000001904");
    static final UUID GRADE_ELEVEN_ARTS_CLUB_ID = UUID.fromString("00000000-0000-0000-0000-000000001905");

    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PEER_ID = UUID.fromString("00000000-0000-0000-0000-000000000211");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    ClubsDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Instant now = clock.instant();
        insertClub(OPEN_ACADEMIC_CLUB_ID, ClubCategory.ACADEMIC, "CLB Khoa học và Sáng tạo",
                "Thực hiện dự án STEM, nghiên cứu khoa học và tham gia các cuộc thi sáng tạo.",
                "Cô Nguyễn Thu Hà", "Thứ Tư, 16:00–17:30", "Phòng Lab STEM",
                ClubAudience.ALL, null, 50, now.plus(Duration.ofDays(30)), true, now);
        insertClub(ACTIVE_SPORTS_CLUB_ID, ClubCategory.SPORTS, "CLB Bóng rổ",
                "Rèn luyện kỹ thuật bóng rổ và thi đấu giao hữu giữa các lớp.",
                "Thầy Trần Minh Đức", "Thứ Ba và Thứ Sáu, 16:15–17:30", "Nhà đa năng",
                ClubAudience.ALL, null, 30, now.plus(Duration.ofDays(20)), true, now);
        insertClub(PENDING_MEDIA_CLUB_ID, ClubCategory.MEDIA, "CLB Truyền thông",
                "Sản xuất nội dung, nhiếp ảnh và truyền thông cho hoạt động của trường.",
                "Cô Lê Hoài An", "Thứ Năm, 16:00–17:30", "Phòng C.302",
                ClubAudience.ALL, null, 25, now.plus(Duration.ofDays(15)), true, now);
        insertClub(FULL_COMMUNITY_CLUB_ID, ClubCategory.COMMUNITY, "CLB Tình nguyện",
                "Tổ chức hoạt động cộng đồng và dự án thiện nguyện của học sinh.",
                "Thầy Phạm Quốc Huy", "Thứ Bảy, 08:00–10:00", "Phòng Đoàn trường",
                ClubAudience.ALL, null, 1, now.plus(Duration.ofDays(10)), true, now);
        insertClub(GRADE_ELEVEN_ARTS_CLUB_ID, ClubCategory.ARTS, "CLB Sân khấu khối 11",
                "Biểu diễn sân khấu dành riêng cho học sinh khối 11.",
                "Cô Vũ Ngọc Mai", "Thứ Hai, 16:00–17:30", "Hội trường A",
                ClubAudience.GRADE, 11, 40, now.plus(Duration.ofDays(25)), true, now);

        insertMembership("00000000-0000-0000-0000-000000001951", ACTIVE_SPORTS_CLUB_ID,
                STUDENT_ID, ClubMembershipStatus.ACTIVE, now.minus(Duration.ofDays(20)));
        insertMembership("00000000-0000-0000-0000-000000001952", PENDING_MEDIA_CLUB_ID,
                STUDENT_ID, ClubMembershipStatus.PENDING, now.minus(Duration.ofDays(2)));
        insertMembership("00000000-0000-0000-0000-000000001953", FULL_COMMUNITY_CLUB_ID,
                PEER_ID, ClubMembershipStatus.ACTIVE, now.minus(Duration.ofDays(15)));
    }

    private void insertClub(UUID id, ClubCategory category, String name, String description,
            String advisor, String schedule, String location, ClubAudience audience,
            Integer grade, Integer capacity, Instant deadline, boolean accepting, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO school_clubs (
                    id, category, name, description, advisor_name, meeting_schedule, location,
                    audience, audience_grade_level, capacity, application_deadline,
                    accepting_applications, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, id, category.name(), name, description, advisor, schedule, location,
                audience.name(), grade, capacity, Timestamp.from(deadline), accepting,
                Timestamp.from(now), Timestamp.from(now));
    }

    private void insertMembership(String id, UUID clubId, UUID studentId,
            ClubMembershipStatus status, Instant appliedAt) {
        jdbcTemplate.update("""
                INSERT INTO student_club_memberships (
                    id, club_id, student_id, status, applied_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (club_id, student_id) DO NOTHING
                """, UUID.fromString(id), clubId, studentId, status.name(),
                Timestamp.from(appliedAt), Timestamp.from(appliedAt));
    }
}
