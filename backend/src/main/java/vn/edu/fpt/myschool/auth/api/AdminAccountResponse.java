package vn.edu.fpt.myschool.auth.api;

import java.util.UUID;

import vn.edu.fpt.myschool.auth.application.AuthenticatedAdmin;
import vn.edu.fpt.myschool.auth.domain.AdminProfile;
import vn.edu.fpt.myschool.auth.domain.UserRole;

public record AdminAccountResponse(
        UUID id,
        String fullName,
        String role) {

    static AdminAccountResponse from(AuthenticatedAdmin admin) {
        return from(admin.profile());
    }

    // This response only ever describes an admin session, so the role it reports is fixed
    // rather than derived from the account's full role set.
    static AdminAccountResponse from(AdminProfile profile) {
        return new AdminAccountResponse(profile.id(), profile.fullName(), UserRole.ADMIN.name());
    }
}
