package vn.edu.fpt.myschool.admin.dashboard.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.admin.dashboard.application.port.AdminDashboardStore;
import vn.edu.fpt.myschool.admin.dashboard.domain.AdminDashboard;

@Service
public class AdminDashboardService {

    private static final Duration RECENT_GRADE_WINDOW = Duration.ofDays(7);
    private static final int RECENT_ACTIVITY_LIMIT = 6;

    private final AdminDashboardStore dashboardStore;
    private final Clock clock;

    public AdminDashboardService(AdminDashboardStore dashboardStore, Clock clock) {
        this.dashboardStore = dashboardStore;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminDashboard getDashboard() {
        Instant now = clock.instant();
        return new AdminDashboard(
                dashboardStore.loadMetrics(now, now.minus(RECENT_GRADE_WINDOW)),
                dashboardStore.findRecentActivities(RECENT_ACTIVITY_LIMIT),
                now);
    }
}
