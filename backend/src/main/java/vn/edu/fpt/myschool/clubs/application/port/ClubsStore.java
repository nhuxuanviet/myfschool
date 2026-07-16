package vn.edu.fpt.myschool.clubs.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.clubs.domain.ClubCategory;
import vn.edu.fpt.myschool.clubs.domain.ClubMembership;
import vn.edu.fpt.myschool.clubs.domain.ClubProjection;
import vn.edu.fpt.myschool.clubs.domain.SchoolClub;

public interface ClubsStore {
    List<ClubProjection> findVisible(UUID studentId, int gradeLevel, ClubCategory category);

    Optional<ClubProjection> findVisibleById(UUID clubId, UUID studentId, int gradeLevel);

    Optional<SchoolClub> lockVisibleById(UUID clubId, int gradeLevel);

    Optional<ClubMembership> findMembership(UUID clubId, UUID studentId);

    int countActiveMembers(UUID clubId);

    void createApplication(UUID id, UUID clubId, UUID studentId, Instant appliedAt);

    void reactivateApplication(UUID clubId, UUID studentId, Instant appliedAt);

    void withdrawApplication(UUID clubId, UUID studentId, Instant withdrawnAt);
}
