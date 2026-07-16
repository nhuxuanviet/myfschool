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

    static AdminIdentityException parentNotFound() {
        return new AdminIdentityException(
                HttpStatus.NOT_FOUND, "PARENT_NOT_FOUND", "Guardian does not exist");
    }

    static AdminIdentityException staleParent() {
        return new AdminIdentityException(
                HttpStatus.CONFLICT,
                "PARENT_VERSION_CONFLICT",
                "Guardian was changed by someone else. Reload and try again");
    }

    static AdminIdentityException phoneNumberTaken() {
        return new AdminIdentityException(
                HttpStatus.CONFLICT, "PHONE_NUMBER_TAKEN", "Phone number already has an account");
    }

    static AdminIdentityException studentNotFound() {
        return new AdminIdentityException(
                HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "Student does not exist");
    }

    static AdminIdentityException linkAlreadyInForce() {
        return new AdminIdentityException(
                HttpStatus.CONFLICT,
                "GUARDIAN_LINK_EXISTS",
                "This guardian is already linked to this student");
    }

    static AdminIdentityException linkNotInForce() {
        return new AdminIdentityException(
                HttpStatus.CONFLICT,
                "GUARDIAN_LINK_NOT_IN_FORCE",
                "Link does not exist, has already ended, or would end before it started");
    }

    static AdminIdentityException weakPassword() {
        return new AdminIdentityException(
                HttpStatus.BAD_REQUEST,
                "WEAK_PASSWORD",
                "Initial password does not meet the password policy");
    }

    static AdminIdentityException invalidPhoneNumber() {
        return new AdminIdentityException(
                HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER", "Phone number is not a valid Vietnamese number");
    }
}
