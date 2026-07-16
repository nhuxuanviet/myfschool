package vn.edu.fpt.myschool.home.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.home.application.HomeDashboardService;

@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "Authenticated student home dashboard")
@SecurityRequirement(name = "bearerAuth")
public class HomeController {

    private final HomeDashboardService homeDashboardService;

    public HomeController(HomeDashboardService homeDashboardService) {
        this.homeDashboardService = homeDashboardService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated student's home dashboard")
    public HomeResponse getHome(@AuthenticationPrincipal Jwt jwt) {
        return HomeResponse.from(homeDashboardService.getDashboard(jwt.getSubject()));
    }
}
