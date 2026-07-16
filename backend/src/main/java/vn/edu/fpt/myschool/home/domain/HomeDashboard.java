package vn.edu.fpt.myschool.home.domain;

import java.util.List;
import java.util.Objects;

public record HomeDashboard(
        HomeStudent student,
        AcademicTerm academicTerm,
        HomeSummary summary,
        List<HomeAnnouncement> announcements) {

    public HomeDashboard {
        Objects.requireNonNull(student, "student must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        announcements = List.copyOf(Objects.requireNonNull(announcements, "announcements must not be null"));
    }
}
