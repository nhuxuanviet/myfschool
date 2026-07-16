package vn.edu.fpt.myschool.forms.application;

import org.springframework.http.HttpStatus;

import vn.edu.fpt.myschool.shared.error.ApiException;

final class FormsException extends ApiException {

    private FormsException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    static FormsException invalidAuthenticatedSubject() {
        return new FormsException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication token is invalid");
    }

    static FormsException studentProfileNotFound() {
        return new FormsException(HttpStatus.NOT_FOUND, "STUDENT_PROFILE_NOT_FOUND", "Student profile was not found");
    }

    static FormsException formNotFound() {
        return new FormsException(HttpStatus.NOT_FOUND, "FORM_NOT_FOUND", "The requested student form is unavailable");
    }

    static FormsException invalidDates() {
        return new FormsException(
                HttpStatus.BAD_REQUEST,
                "FORM_DATE_RANGE_INVALID",
                "Leave forms require a valid date range and other form types must not contain dates");
    }

    static FormsException invalidPayload() {
        return new FormsException(
                HttpStatus.BAD_REQUEST,
                "FORM_PAYLOAD_INVALID",
                "Form type and a reason of at most 1000 characters are required");
    }

    static FormsException cannotCancel() {
        return new FormsException(
                HttpStatus.CONFLICT,
                "FORM_CANNOT_BE_CANCELLED",
                "This student form can no longer be cancelled");
    }
}
