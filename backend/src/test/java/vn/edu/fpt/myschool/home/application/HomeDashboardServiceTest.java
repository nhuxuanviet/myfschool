package vn.edu.fpt.myschool.home.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.home.application.port.HomeDashboardStore;
import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.home.domain.HomeAnnouncement;
import vn.edu.fpt.myschool.home.domain.HomeDashboard;

class HomeDashboardServiceTest {

    @Test
    void resolvesTheActiveTermUsingTheVietnameseSchoolDateAtAUtcBoundary() {
        UUID userId = UUID.randomUUID();
        Clock utcClockAtSundayEvening = Clock.fixed(
                Instant.parse("2026-07-12T18:30:00Z"), ZoneOffset.UTC);
        StudentProfileStore students = ignored -> Optional.of(new StudentProfile(
                UUID.randomUUID(),
                "SE1913001",
                "Nguyễn Minh Anh",
                10,
                "10A1"));
        AtomicReference<LocalDate> requestedTermDate = new AtomicReference<>();
        HomeDashboardStore store = new HomeDashboardStore() {
            @Override
            public Optional<AcademicTerm> findActiveAcademicTerm(LocalDate date) {
                requestedTermDate.set(date);
                return Optional.of(new AcademicTerm(
                        "2026-2027",
                        "HK1",
                        "Học kỳ I",
                        LocalDate.of(2026, 7, 13),
                        LocalDate.of(2026, 12, 31)));
            }

            @Override
            public int countUpcomingVisibleEvents(int gradeLevel, Instant viewedAt) {
                return 3;
            }

            @Override
            public int countPendingStudentForms(UUID studentId) {
                return 2;
            }

            @Override
            public int countActiveClubMemberships(UUID studentId) {
                return 1;
            }

            @Override
            public List<HomeAnnouncement> findVisibleAnnouncements(int gradeLevel, Instant viewedAt) {
                return List.of();
            }
        };
        HomeDashboardService service = new HomeDashboardService(students, store, utcClockAtSundayEvening);

        HomeDashboard dashboard = service.getDashboard(userId.toString());

        // 18:30 Sunday UTC is 01:30 Monday in Asia/Ho_Chi_Minh.
        assertThat(requestedTermDate.get()).isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(dashboard.academicTerm().code()).isEqualTo("HK1");
        assertThat(dashboard.summary().events().upcoming()).isEqualTo(3);
        assertThat(dashboard.summary().forms().pending()).isEqualTo(2);
        assertThat(dashboard.summary().clubs().active()).isEqualTo(1);
    }
}
