package vn.edu.fpt.myschool.home.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.home.domain.AcademicTerm;
import vn.edu.fpt.myschool.home.domain.HomeAnnouncement;

/**
 * Read model backing the student home dashboard. Implementations must use a
 * bounded number of queries, independent of the number of announcements.
 */
public interface HomeDashboardStore {

    Optional<AcademicTerm> findActiveAcademicTerm(LocalDate date);

    int countUpcomingVisibleEvents(int gradeLevel, Instant viewedAt);

    int countPendingStudentForms(UUID studentId);

    int countActiveClubMemberships(UUID studentId);

    List<HomeAnnouncement> findVisibleAnnouncements(int gradeLevel, Instant viewedAt);
}
