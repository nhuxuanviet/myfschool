package vn.edu.fpt.myschool.admin.identity.api;

import java.time.LocalDate;
import java.util.UUID;

import vn.edu.fpt.myschool.admin.identity.domain.AdminIdentity;

public record AdminGuardianLinkResponse(
        UUID id,
        UUID parentId,
        String parentFullName,
        UUID studentId,
        String studentFullName,
        String studentCode,
        String relationship,
        int contactOrder,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean inForce) {

    static AdminGuardianLinkResponse from(AdminIdentity.GuardianLink link) {
        return new AdminGuardianLinkResponse(
                link.id(),
                link.parentId(),
                link.parentFullName(),
                link.studentId(),
                link.studentFullName(),
                link.studentCode(),
                link.relationship().name(),
                link.contactOrder(),
                link.effectiveFrom(),
                link.effectiveTo(),
                link.isInForce());
    }
}
