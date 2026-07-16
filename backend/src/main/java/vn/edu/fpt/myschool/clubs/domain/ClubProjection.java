package vn.edu.fpt.myschool.clubs.domain;

import java.util.Objects;

public record ClubProjection(
        SchoolClub club,
        int activeMemberCount,
        ClubMembershipStatus membershipStatus) {

    public ClubProjection {
        Objects.requireNonNull(club, "club must not be null");
        Objects.requireNonNull(membershipStatus, "membershipStatus must not be null");
        if (activeMemberCount < 0) {
            throw new IllegalArgumentException("activeMemberCount must not be negative");
        }
    }
}
