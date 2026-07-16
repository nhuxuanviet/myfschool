package vn.edu.fpt.myschool.clubs.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.myschool.auth.application.port.StudentProfileStore;
import vn.edu.fpt.myschool.auth.domain.StudentProfile;
import vn.edu.fpt.myschool.clubs.application.port.ClubsStore;
import vn.edu.fpt.myschool.clubs.domain.ClubApplicationResult;
import vn.edu.fpt.myschool.clubs.domain.ClubCategory;
import vn.edu.fpt.myschool.clubs.domain.ClubDetails;
import vn.edu.fpt.myschool.clubs.domain.ClubMembership;
import vn.edu.fpt.myschool.clubs.domain.ClubMembershipStatus;
import vn.edu.fpt.myschool.clubs.domain.ClubProjection;
import vn.edu.fpt.myschool.clubs.domain.SchoolClub;

@Service
public class ClubsService {
    private final StudentProfileStore studentProfileStore;
    private final ClubsStore clubsStore;
    private final Clock clock;

    public ClubsService(StudentProfileStore studentProfileStore, ClubsStore clubsStore, Clock clock) {
        this.studentProfileStore = studentProfileStore;
        this.clubsStore = clubsStore;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ClubDetails> getClubs(String subject, ClubCategory category) {
        StudentProfile student = authenticatedStudent(subject);
        Instant now = clock.instant();
        return clubsStore.findVisible(student.id(), student.gradeLevel(), category)
                .stream().map(projection -> toDetails(projection, now)).toList();
    }

    @Transactional(readOnly = true)
    public ClubDetails getClub(String subject, UUID clubId) {
        StudentProfile student = authenticatedStudent(subject);
        Instant now = clock.instant();
        return toDetails(clubsStore.findVisibleById(clubId, student.id(), student.gradeLevel())
                .orElseThrow(ClubsException::clubNotFound), now);
    }

    @Transactional
    public ClubApplicationResult apply(String subject, UUID clubId) {
        StudentProfile student = authenticatedStudent(subject);
        SchoolClub club = clubsStore.lockVisibleById(clubId, student.gradeLevel())
                .orElseThrow(ClubsException::clubNotFound);
        Instant now = clock.instant();
        ClubMembership existing = clubsStore.findMembership(club.id(), student.id()).orElse(null);
        if (existing != null && (existing.status() == ClubMembershipStatus.PENDING
                || existing.status() == ClubMembershipStatus.ACTIVE)) {
            throw ClubsException.alreadyApplied();
        }
        int activeCount = clubsStore.countActiveMembers(club.id());
        assertApplicationsOpen(club, activeCount, now);
        boolean created = existing == null;
        if (created) {
            clubsStore.createApplication(UUID.randomUUID(), club.id(), student.id(), now);
        } else {
            clubsStore.reactivateApplication(club.id(), student.id(), now);
        }
        return new ClubApplicationResult(
                toDetails(new ClubProjection(club, activeCount, ClubMembershipStatus.PENDING), now),
                created);
    }

    @Transactional
    public ClubDetails withdraw(String subject, UUID clubId) {
        StudentProfile student = authenticatedStudent(subject);
        SchoolClub club = clubsStore.lockVisibleById(clubId, student.gradeLevel())
                .orElseThrow(ClubsException::clubNotFound);
        Instant now = clock.instant();
        clubsStore.findMembership(club.id(), student.id())
                .filter(membership -> membership.status() == ClubMembershipStatus.PENDING)
                .orElseThrow(ClubsException::pendingApplicationNotFound);
        clubsStore.withdrawApplication(club.id(), student.id(), now);
        return toDetails(new ClubProjection(
                club,
                clubsStore.countActiveMembers(club.id()),
                ClubMembershipStatus.WITHDRAWN), now);
    }

    private ClubDetails toDetails(ClubProjection projection, Instant now) {
        boolean open = applicationsOpen(projection.club(), projection.activeMemberCount(), now);
        boolean canApply = projection.membershipStatus() != ClubMembershipStatus.PENDING
                && projection.membershipStatus() != ClubMembershipStatus.ACTIVE
                && open;
        boolean canWithdraw = projection.membershipStatus() == ClubMembershipStatus.PENDING;
        return new ClubDetails(projection.club(), projection.activeMemberCount(),
                projection.membershipStatus(), canApply, canWithdraw);
    }

    private void assertApplicationsOpen(SchoolClub club, int activeCount, Instant now) {
        if (!club.acceptingApplications()
                || (club.applicationDeadline() != null && now.isAfter(club.applicationDeadline()))) {
            throw ClubsException.applicationsClosed();
        }
        if (club.capacity() != null && activeCount >= club.capacity()) {
            throw ClubsException.capacityReached();
        }
    }

    private boolean applicationsOpen(SchoolClub club, int activeCount, Instant now) {
        return club.acceptingApplications()
                && (club.applicationDeadline() == null || !now.isAfter(club.applicationDeadline()))
                && (club.capacity() == null || activeCount < club.capacity());
    }

    private StudentProfile authenticatedStudent(String subject) {
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw ClubsException.invalidAuthenticatedSubject();
        }
        return studentProfileStore.findByUserId(userId).orElseThrow(ClubsException::studentNotFound);
    }
}
