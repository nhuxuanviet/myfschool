package vn.edu.fpt.myschool.shared.error;

import java.util.Objects;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.code = requireText(code, "code");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
