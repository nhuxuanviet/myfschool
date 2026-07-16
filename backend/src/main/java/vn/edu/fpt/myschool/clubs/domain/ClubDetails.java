package vn.edu.fpt.myschool.clubs.domain;

import java.util.Objects;

public record ClubDetails(
        SchoolClub club,
        int activeMemberCount,
        ClubMembershipStatus membershipStatus,
        boolean canApply,
        boolean canWithdraw) {

    public ClubDetails {
        Objects.requireNonNull(club, "club must not be null");
        Objects.requireNonNull(membershipStatus, "membershipStatus must not be null");
        if (activeMemberCount < 0 || (canApply && canWithdraw)) {
            throw new IllegalArgumentException("Invalid club action state");
        }
    }
}
