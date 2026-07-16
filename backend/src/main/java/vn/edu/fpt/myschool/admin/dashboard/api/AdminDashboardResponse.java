package vn.edu.fpt.myschool.admin.dashboard.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.dashboard.domain.AdminDashboard;

public record AdminDashboardResponse(
        MetricsResponse metrics,
        List<ActivityResponse> recentActivities,
        Instant generatedAt) {

    static AdminDashboardResponse from(AdminDashboard dashboard) {
        return new AdminDashboardResponse(
                MetricsResponse.from(dashboard.metrics()),
                dashboard.recentActivities().stream().map(ActivityResponse::from).toList(),
                dashboard.generatedAt());
    }

    public record MetricsResponse(
            long totalStudents,
            long activeClasses,
            long pendingForms,
            long upcomingEvents,
            long pendingClubApplications,
            long recentlyUpdatedGrades) {

        static MetricsResponse from(AdminDashboard.Metrics metrics) {
            return new MetricsResponse(
                    metrics.totalStudents(),
                    metrics.activeClasses(),
                    metrics.pendingForms(),
                    metrics.upcomingEvents(),
                    metrics.pendingClubApplications(),
                    metrics.recentlyUpdatedGrades());
        }
    }

    public record ActivityResponse(
            UUID id,
            String eventType,
            String actorName,
            Instant occurredAt) {

        static ActivityResponse from(AdminDashboard.Activity activity) {
            return new ActivityResponse(
                    activity.id(),
                    activity.eventType(),
                    activity.actorName(),
                    activity.occurredAt());
        }
    }
}
