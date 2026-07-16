package vn.edu.fpt.myschool.admin.dashboard.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminDashboard(
        Metrics metrics,
        List<Activity> recentActivities,
        Instant generatedAt) {

    public AdminDashboard {
        recentActivities = List.copyOf(recentActivities);
    }

    public record Metrics(
            long totalStudents,
            long activeClasses,
            long pendingForms,
            long upcomingEvents,
            long pendingClubApplications,
            long recentlyUpdatedGrades) {
    }

    public record Activity(
            UUID id,
            String eventType,
            String actorName,
            Instant occurredAt) {
    }
}
