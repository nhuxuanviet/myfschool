package vn.edu.fpt.myschool.admin.identity.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

public final class AdminIdentityException extends ApiException {

    private AdminIdentityException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static AdminIdentityException teacherNotFound() {
        return new AdminIdentityException(
                HttpStatus.NOT_FOUND, "TEACHER_NOT_FOUND", "Teacher does not exist");
    }

    static AdminIdentityException teacherCodeTaken() {
        return new AdminIdentityException(
                HttpStatus.CONFLICT, "TEACHER_CODE_TAKEN", "Teacher code is already in use");
    }

    /** The row moved on since the caller read it, so the update would silently discard a change. */
    static AdminIdentityException staleTeacher() {
        return new AdminIdentityException(
                HttpStatus.CONFLICT,
                "TEACHER_VERSION_CONFLICT",
                "Teacher was changed by someone else. Reload and try again");
    }
}
