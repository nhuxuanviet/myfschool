package vn.edu.fpt.myschool.auth.application;

import java.util.Objects;

import vn.edu.fpt.myschool.auth.domain.AdminProfile;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserAccount account,
        UserRole activeRole,
        UserProfile profile) {

    public AuthenticationResult {
        Objects.requireNonNull(account, "account must not be null");
        Objects.requireNonNull(activeRole, "activeRole must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        if (!account.hasRole(activeRole)) {
            throw new IllegalArgumentException("Account does not hold the active role");
        }
    }

    public StudentProfile student() {
        if (profile instanceof StudentProfile studentProfile) {
            return studentProfile;
        }
        throw new IllegalStateException("Authenticated session is not a student session");
    }

    public AdminProfile admin() {
        if (profile instanceof AdminProfile adminProfile) {
            return adminProfile;
        }
        throw new IllegalStateException("Authenticated session is not an admin session");
    }
}
