package vn.edu.fpt.myschool.clubs.domain;

import java.util.Objects;
import java.util.UUID;

public record ClubMembership(UUID id, ClubMembershipStatus status) {
    public ClubMembership {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (status == ClubMembershipStatus.NOT_APPLIED) {
            throw new IllegalArgumentException("Persisted membership cannot be NOT_APPLIED");
        }
    }
}
