package vn.edu.fpt.myschool.auth.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.auth.application.AdminAuthService;
import vn.edu.fpt.myschool.auth.application.AuthenticationResult;
import vn.edu.fpt.myschool.auth.infrastructure.security.AdminSessionCookieService;

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "Admin authentication", description = "Secure administrator sessions")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminSessionCookieService cookieService;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            AdminSessionCookieService cookieService) {
        this.adminAuthService = adminAuthService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate an administrator")
    public AdminSessionResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthenticationResult result =
                adminAuthService.login(request.phoneNumber(), request.password());
        String csrfToken = cookieService.issue(response, result.refreshToken());
        return AdminSessionResponse.from(result, csrfToken);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the administrator refresh session")
    public AdminSessionResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        cookieService.requireValidCsrf(request);
        AuthenticationResult result =
                adminAuthService.refresh(cookieService.requireRefreshToken(request));
        String csrfToken = cookieService.issue(response, result.refreshToken());
        return AdminSessionResponse.from(result, csrfToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "End the administrator session")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        cookieService.requireValidCsrf(request);
        adminAuthService.logout(cookieService.requireRefreshToken(request));
        cookieService.clear(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Return the current administrator account")
    public AdminAccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return AdminAccountResponse.from(
                adminAuthService.getAccount(UUID.fromString(jwt.getSubject())));
    }
}
