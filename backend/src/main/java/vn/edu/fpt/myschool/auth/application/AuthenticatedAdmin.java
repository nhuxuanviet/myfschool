package vn.edu.fpt.myschool.auth.application;

import java.util.Objects;

import vn.edu.fpt.myschool.auth.domain.AdminProfile;
import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserRole;

/** An admin account together with the profile it holds for the admin role. */
public record AuthenticatedAdmin(UserAccount account, AdminProfile profile) {

    public AuthenticatedAdmin {
        Objects.requireNonNull(account, "account must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        if (!account.hasRole(UserRole.ADMIN)) {
            throw new IllegalArgumentException("Account does not hold the admin role");
        }
    }
}
