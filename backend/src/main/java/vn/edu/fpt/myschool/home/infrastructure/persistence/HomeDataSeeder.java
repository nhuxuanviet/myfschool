package vn.edu.fpt.myschool.home.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.home.domain.AnnouncementAudience;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

/**
 * Stable demo identifiers and content, with dates anchored to the injected
 * clock so a fresh dev/e2e dashboard has active sample data on any date.
 */
@Component
@Profile("(dev | e2e) & !prod")
@Order(200)
class HomeDataSeeder implements ApplicationRunner {

    private static final UUID SEEDED_ACADEMIC_YEAR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID SEEDED_ACADEMIC_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID HISTORICAL_ACADEMIC_YEAR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID HISTORICAL_ACADEMIC_TERM_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID WELCOME_ANNOUNCEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID GRADE_TEN_ANNOUNCEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID FUTURE_ANNOUNCEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000403");
    private static final UUID EXPIRED_ANNOUNCEMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000404");

    private final AcademicYearJpaRepository academicYearRepository;
    private final AcademicTermJpaRepository academicTermRepository;
    private final AnnouncementJpaRepository announcementRepository;
    private final Clock clock;

    HomeDataSeeder(
            AcademicYearJpaRepository academicYearRepository,
            AcademicTermJpaRepository academicTermRepository,
            AnnouncementJpaRepository announcementRepository,
            Clock clock) {
        this.academicYearRepository = academicYearRepository;
        this.academicTermRepository = academicTermRepository;
        this.announcementRepository = announcementRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, SchoolTimeZone.ZONE);
        seedAcademicYear(today, now);
        seedAcademicTerm(today, now);
        seedHistoricalAcademicYear(today, now);
        seedHistoricalAcademicTerm(today, now);
        seedAnnouncements(now);
    }

    private void seedAcademicYear(LocalDate today, Instant now) {
        if (academicYearRepository.existsById(SEEDED_ACADEMIC_YEAR_ID)) {
            return;
        }
        LocalDate startsOn = today.minusMonths(6);
        LocalDate endsOn = today.plusMonths(6);
        academicYearRepository.save(new AcademicYearJpaEntity(
                SEEDED_ACADEMIC_YEAR_ID,
                "%d-%d".formatted(startsOn.getYear(), endsOn.getYear()),
                startsOn,
                endsOn,
                now));
    }

    private void seedAcademicTerm(LocalDate today, Instant now) {
        if (academicTermRepository.existsById(SEEDED_ACADEMIC_TERM_ID)) {
            return;
        }
        academicTermRepository.save(new AcademicTermJpaEntity(
                SEEDED_ACADEMIC_TERM_ID,
                SEEDED_ACADEMIC_YEAR_ID,
                "HK1",
                "Học kỳ I",
                today.minusDays(30),
                today.plusDays(120),
                now));
    }

    private void seedHistoricalAcademicYear(LocalDate today, Instant now) {
        if (academicYearRepository.existsById(HISTORICAL_ACADEMIC_YEAR_ID)) {
            return;
        }
        LocalDate currentAcademicYearStartsOn = today.minusMonths(6);
        LocalDate startsOn = currentAcademicYearStartsOn.minusYears(1);
        LocalDate endsOn = currentAcademicYearStartsOn.minusDays(1);
        academicYearRepository.save(new AcademicYearJpaEntity(
                HISTORICAL_ACADEMIC_YEAR_ID,
                "%d-%d".formatted(startsOn.getYear(), endsOn.getYear()),
                startsOn,
                endsOn,
                now));
    }

    private void seedHistoricalAcademicTerm(LocalDate today, Instant now) {
        if (academicTermRepository.existsById(HISTORICAL_ACADEMIC_TERM_ID)) {
            return;
        }
        LocalDate currentAcademicYearStartsOn = today.minusMonths(6);
        LocalDate startsOn = currentAcademicYearStartsOn.minusMonths(6);
        LocalDate endsOn = currentAcademicYearStartsOn.minusDays(1);
        academicTermRepository.save(new AcademicTermJpaEntity(
                HISTORICAL_ACADEMIC_TERM_ID,
                HISTORICAL_ACADEMIC_YEAR_ID,
                "HK2",
                "Học kỳ II",
                startsOn,
                endsOn,
                now));
    }

    private void seedAnnouncements(Instant now) {
        saveAnnouncementIfMissing(new AnnouncementJpaEntity(
                WELCOME_ANNOUNCEMENT_ID,
                "Chào mừng đến MySchool",
                "Chúc các em có một học kỳ học tập hiệu quả.",
                AnnouncementAudience.ALL,
                null,
                now.minus(Duration.ofHours(1)),
                now.minus(Duration.ofDays(1)),
                null,
                now));
        saveAnnouncementIfMissing(new AnnouncementJpaEntity(
                GRADE_TEN_ANNOUNCEMENT_ID,
                "Thông báo dành cho khối 10",
                "Học sinh khối 10 vui lòng hoàn thành thông tin đầu năm.",
                AnnouncementAudience.GRADE,
                10,
                now.minus(Duration.ofHours(2)),
                now.minus(Duration.ofDays(1)),
                null,
                now));
        saveAnnouncementIfMissing(new AnnouncementJpaEntity(
                FUTURE_ANNOUNCEMENT_ID,
                "Thông báo chưa đến thời điểm công bố",
                "Nội dung này không được trả về trước thời điểm hiển thị.",
                AnnouncementAudience.ALL,
                null,
                now.plus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(1)),
                null,
                now));
        saveAnnouncementIfMissing(new AnnouncementJpaEntity(
                EXPIRED_ANNOUNCEMENT_ID,
                "Thông báo đã hết hạn",
                "Nội dung này không được trả về sau khi hết thời gian hiển thị.",
                AnnouncementAudience.ALL,
                null,
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofMinutes(1)),
                now));
    }

    private void saveAnnouncementIfMissing(AnnouncementJpaEntity announcement) {
        if (!announcementRepository.existsById(announcement.getId())) {
            announcementRepository.save(announcement);
        }
    }
}
