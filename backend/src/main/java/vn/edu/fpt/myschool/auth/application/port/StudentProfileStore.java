package vn.edu.fpt.myschool.auth.application.port;

import java.util.Optional;
import java.util.UUID;

import vn.edu.fpt.myschool.auth.domain.StudentProfile;

/**
 * Read access to a student's profile for capabilities that are authorized by
 * the authenticated user account rather than by a client supplied student id.
 */
public interface StudentProfileStore {

    Optional<StudentProfile> findByUserId(UUID userId);
}
