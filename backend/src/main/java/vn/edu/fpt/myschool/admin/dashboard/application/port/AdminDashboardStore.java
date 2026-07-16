package vn.edu.fpt.myschool.admin.dashboard.application.port;

import java.time.Instant;
import java.util.List;

import vn.edu.fpt.myschool.admin.dashboard.domain.AdminDashboard;

public interface AdminDashboardStore {

    AdminDashboard.Metrics loadMetrics(Instant now, Instant recentGradeCutoff);

    List<AdminDashboard.Activity> findRecentActivities(int limit);
}
