package vn.edu.fpt.myschool.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.fpt.myschool.auth.application.AuthenticationResult;
import vn.edu.fpt.myschool.auth.application.AuthService;
import vn.edu.fpt.myschool.auth.application.PasswordResetDeliveryUnavailableException;
import vn.edu.fpt.myschool.auth.application.PasswordResetService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Student authentication and password recovery")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a student, teacher or parent")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result =
                authService.login(request.phoneNumber(), request.password(), request.role());
        return AuthTokenResponse.from(result, authService.mobileRolesOf(result.account()));
    }

    @PostMapping("/switch-role")
    @Operation(summary = "Open a session for another role the account already holds")
    public AuthTokenResponse switchRole(@Valid @RequestBody SwitchRoleRequest request) {
        AuthenticationResult result =
                authService.switchRole(request.refreshToken(), request.role());
        return AuthTokenResponse.from(result, authService.mobileRolesOf(result.account()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token and issue a new token pair")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResult result = authService.refresh(request.refreshToken());
        return AuthTokenResponse.from(result, authService.mobileRolesOf(result.account()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh-token family")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Request a password-reset challenge")
    public ResponseEntity<PasswordResetChallengeResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        try {
            return ResponseEntity.accepted().body(PasswordResetChallengeResponse.from(
                    passwordResetService.request(request.phoneNumber())));
        } catch (PasswordResetDeliveryUnavailableException exception) {
            LOGGER.warn(
                    "Password-reset OTP delivery unavailable for challenge {}",
                    exception.challenge().challengeId());
            return ResponseEntity.accepted().body(
                    PasswordResetChallengeResponse.from(exception.challenge()));
        }
    }

    @PostMapping("/password-reset/verify")
    @Operation(summary = "Verify a password-reset OTP")
    public PasswordResetVerificationResponse verifyPasswordReset(
            @Valid @RequestBody PasswordResetVerifyRequest request) {
        return PasswordResetVerificationResponse.from(
                passwordResetService.verify(request.challengeId(), request.otp()));
    }

    @PostMapping("/password-reset/complete")
    @Operation(summary = "Set a new password using a verified reset token")
    public ResponseEntity<Void> completePasswordReset(
            @Valid @RequestBody PasswordResetCompleteRequest request) {
        passwordResetService.complete(request.resetToken(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
