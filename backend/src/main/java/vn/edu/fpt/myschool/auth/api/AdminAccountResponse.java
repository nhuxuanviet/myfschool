package vn.edu.fpt.myschool.auth.api;

import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.UserAccount;

public record AdminAccountResponse(
        UUID id,
        String fullName,
        String role) {

    static AdminAccountResponse from(UserAccount account) {
        return new AdminAccountResponse(
                account.admin().id(),
                account.admin().fullName(),
                account.role().name());
    }
}
