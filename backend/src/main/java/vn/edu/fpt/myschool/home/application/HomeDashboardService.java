package vn.edu.fpt.myschool.home.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.home.application.port.HomeDashboardStore;
import vn.edu.fpt.myschool.home.domain.HomeDashboard;
import vn.edu.fpt.myschool.home.domain.HomeStudent;
import vn.edu.fpt.myschool.home.domain.HomeSummary;
import vn.edu.fpt.myschool.shared.time.SchoolTimeZone;

@Service
public class HomeDashboardService {

    private final StudentProfileStore studentProfileStore;
    private final HomeDashboardStore homeDashboardStore;
    private final Clock clock;

    public HomeDashboardService(
            StudentProfileStore studentProfileStore,
            HomeDashboardStore homeDashboardStore,
            Clock clock) {
        this.studentProfileStore = studentProfileStore;
        this.homeDashboardStore = homeDashboardStore;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HomeDashboard getDashboard(String authenticatedUserId) {
        UUID userId = parseAuthenticatedUserId(authenticatedUserId);
        StudentProfile student = studentProfileStore.findByUserId(userId)
                .orElseThrow(HomeException::studentProfileNotFound);
        Instant viewedAt = clock.instant();
        LocalDate currentDate = LocalDate.ofInstant(viewedAt, SchoolTimeZone.ZONE);

        return new HomeDashboard(
                new HomeStudent(
                        student.studentCode(),
                        student.fullName(),
                        student.gradeLevel(),
                        student.className()),
                homeDashboardStore.findActiveAcademicTerm(currentDate).orElse(null),
                new HomeSummary(
                        new HomeSummary.LessonSummary(0),
                        new HomeSummary.EventSummary(
                                homeDashboardStore.countUpcomingVisibleEvents(
                                        student.gradeLevel(), viewedAt)),
                        new HomeSummary.FormSummary(
                                homeDashboardStore.countPendingStudentForms(student.id())),
                        new HomeSummary.ClubSummary(
                                homeDashboardStore.countActiveClubMemberships(student.id()))),
                homeDashboardStore.findVisibleAnnouncements(student.gradeLevel(), viewedAt));
    }

    private UUID parseAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw HomeException.invalidAuthenticatedSubject();
        }
        try {
            return UUID.fromString(authenticatedUserId);
        } catch (IllegalArgumentException exception) {
            throw HomeException.invalidAuthenticatedSubject();
        }
    }
}
