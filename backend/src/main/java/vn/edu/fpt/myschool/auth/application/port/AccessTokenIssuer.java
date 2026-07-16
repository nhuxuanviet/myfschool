package vn.edu.fpt.myschool.auth.application.port;

import vn.edu.fpt.myschool.auth.domain.UserAccount;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(UserAccount userAccount);

    record IssuedAccessToken(String value, long expiresIn) {
    }
}
