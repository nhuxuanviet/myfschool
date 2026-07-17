package vn.edu.fpt.myschool.parent.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

public final class ParentException extends ApiException {

    private ParentException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static ParentException noParentProfile() {
        return new ParentException(
                HttpStatus.FORBIDDEN,
                "PARENT_PROFILE_MISSING",
                "This account has no active guardian profile");
    }

    /**
     * The guardian asked about a student they are not linked to.
     *
     * <p>403 and not 404, always: telling a guardian that one student id exists and another does
     * not would let them enumerate the school's children one guess at a time. Every student who
     * is not theirs must look exactly alike from here.
     */
    static ParentException notLinked() {
        return new ParentException(
                HttpStatus.FORBIDDEN,
                "NOT_LINKED_TO_STUDENT",
                "You are not a registered guardian of this student");
    }
}
