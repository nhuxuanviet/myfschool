package vn.edu.fpt.myschool.auth.application.port;

import vn.edu.fpt.myschool.auth.domain.UserAccount;
import vn.edu.fpt.myschool.auth.domain.UserRole;

public interface AccessTokenIssuer {

    /**
     * Issues an access token bound to exactly one active role.
     *
     * <p>An account may hold several roles, but a token grants only the role it was issued for.
     * Switching role requires a new token.
     */
    IssuedAccessToken issue(UserAccount userAccount, UserRole activeRole);

    record IssuedAccessToken(String value, long expiresIn) {
    }
}
