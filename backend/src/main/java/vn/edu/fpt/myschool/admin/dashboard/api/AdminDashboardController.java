package vn.edu.fpt.myschool.admin.dashboard.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.admin.dashboard.application.AdminDashboardService;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin dashboard", description = "Administrator operational overview")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get bounded administrator dashboard metrics and activity")
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.from(dashboardService.getDashboard());
    }
}
