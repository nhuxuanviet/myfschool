package vn.edu.fpt.myschool.auth.api;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import vn.edu.fpt.myschool.auth.application.AuthenticationResult;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String activeRole,
        List<String> availableRoles,
        StudentResponse student) {

    /**
     * The {@code student} field is populated only for a student session.
     *
     * <p>It stays in the contract so the existing mobile app and its end-to-end suites keep
     * working unchanged; a teacher or parent session reports null there and is described by
     * {@code activeRole} instead.
     */
    static AuthTokenResponse from(AuthenticationResult result, Set<UserRole> availableRoles) {
        return new AuthTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                result.activeRole().name(),
                availableRoles.stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .map(UserRole::name)
                        .toList(),
                result.profile() instanceof StudentProfile student
                        ? StudentResponse.from(student)
                        : null);
    }
}
