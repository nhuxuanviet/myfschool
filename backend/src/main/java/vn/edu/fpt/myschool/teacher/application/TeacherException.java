package vn.edu.fpt.myschool.teacher.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

public final class TeacherException extends ApiException {

    private TeacherException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    /**
     * The account holds the teacher role but no enabled teacher profile.
     *
     * <p>403 rather than 404: the caller authenticated fine, they simply have no teaching
     * identity to act as.
     */
    static TeacherException noTeacherProfile() {
        return new TeacherException(
                HttpStatus.FORBIDDEN,
                "TEACHER_PROFILE_MISSING",
                "This account has no active teacher profile");
    }

    /** Assigned to the class perhaps, but not to this subject in it. */
    static TeacherException notAssignedToSubject() {
        return new TeacherException(
                HttpStatus.FORBIDDEN,
                "NOT_ASSIGNED_TO_SUBJECT",
                "You are not assigned to this subject in this class and term");
    }

    /**
     * The teacher asked about a class they hold no assignment for.
     *
     * <p>403 and not 404 deliberately: answering "not found" for classes that exist but are
     * someone else's, and "forbidden" for one's own, would let a caller map the school by
     * probing. Every class outside the assignment looks identical from here.
     */
    static TeacherException notAssigned() {
        return new TeacherException(
                HttpStatus.FORBIDDEN,
                "NOT_ASSIGNED_TO_CLASS",
                "You are not assigned to this class");
    }
}
