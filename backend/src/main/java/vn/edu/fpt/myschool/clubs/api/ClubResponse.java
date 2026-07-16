package vn.edu.fpt.myschool.clubs.api;

import java.time.Instant;
import java.util.UUID;

import vn.edu.fpt.myschool.clubs.domain.ClubDetails;
import vn.edu.fpt.myschool.clubs.domain.SchoolClub;

public record ClubResponse(
        UUID id,
        String category,
        String name,
        String description,
        String advisorName,
        String meetingSchedule,
        String location,
        Integer audienceGradeLevel,
        Integer capacity,
        int activeMemberCount,
        Instant applicationDeadline,
        String membershipStatus,
        boolean canApply,
        boolean canWithdraw) {

    static ClubResponse from(ClubDetails details) {
        SchoolClub club = details.club();
        return new ClubResponse(club.id(), club.category().name(), club.name(),
                club.description(), club.advisorName(), club.meetingSchedule(), club.location(),
                club.audienceGradeLevel(), club.capacity(), details.activeMemberCount(),
                club.applicationDeadline(), details.membershipStatus().name(),
                details.canApply(), details.canWithdraw());
    }
}
