package vn.edu.fpt.myschool.teacher.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

public final class GradeBookException extends ApiException {

    private GradeBookException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static GradeBookException notFound() {
        return new GradeBookException(
                HttpStatus.NOT_FOUND, "GRADE_BOOK_NOT_FOUND", "Grade book does not exist");
    }

    /**
     * The book is locked, so the teacher may no longer edit it directly.
     *
     * <p>409 rather than 403: the caller is the right person, the book is simply in a state where
     * a change goes through a request the administration approves (spec §5.5).
     */
    static GradeBookException locked() {
        return new GradeBookException(
                HttpStatus.CONFLICT,
                "GRADE_BOOK_LOCKED",
                "Grade book is locked. Submit a grade-change request instead");
    }

    static GradeBookException stale() {
        return new GradeBookException(
                HttpStatus.CONFLICT,
                "GRADE_BOOK_VERSION_CONFLICT",
                "Grade book was changed by someone else. Reload and try again");
    }

    /** A numeric subject takes a score; a remark subject takes an outcome. Never both, never neither. */
    static GradeBookException wrongResultForMode() {
        return new GradeBookException(
                HttpStatus.BAD_REQUEST,
                "MARK_DOES_NOT_MATCH_SUBJECT_MODE",
                "A numeric subject takes a score and a remark subject takes an outcome");
    }
}
