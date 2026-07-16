package vn.edu.fpt.myschool.shared.error;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred";

    private final ApiProblemDetailFactory problemDetailFactory;

    public GlobalExceptionHandler(ApiProblemDetailFactory problemDetailFactory) {
        this.problemDetailFactory = problemDetailFactory;
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = problemDetailFactory.create(
                exception.getStatus(),
                exception.getMessage(),
                exception.getCode(),
                requestUri(request));
        return ResponseEntity.status(exception.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(ValidationError::field))
                .toList();
        ProblemDetail problemDetail = problemDetailFactory.create(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                ApiErrorCode.VALIDATION_FAILED.name(),
                requestUri(request));
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled request failure", exception);
        ProblemDetail problemDetail = problemDetailFactory.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GENERIC_ERROR_MESSAGE,
                ApiErrorCode.INTERNAL_ERROR.name(),
                requestUri(request));
        return ResponseEntity.internalServerError().body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .distinct()
                .sorted(Comparator.comparing(ValidationError::field))
                .toList();
        ProblemDetail problemDetail = problemDetailFactory.create(
                status,
                "Request validation failed",
                ApiErrorCode.VALIDATION_FAILED.name(),
                requestUri(request));
        problemDetail.setProperty("errors", errors);
        return handleExceptionInternal(exception, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            problemDetailFactory.enrich(
                    problemDetail, defaultCode(statusCode).name(), requestUri(request));
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            problemDetailFactory.enrich(
                    problemDetail, defaultCode(statusCode).name(), requestUri(request));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ApiErrorCode defaultCode(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 401 -> ApiErrorCode.UNAUTHORIZED;
            case 403 -> ApiErrorCode.ACCESS_DENIED;
            case 404 -> ApiErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ApiErrorCode.METHOD_NOT_ALLOWED;
            case 409 -> ApiErrorCode.CONFLICT;
            default -> statusCode.is4xxClientError()
                    ? ApiErrorCode.BAD_REQUEST
                    : ApiErrorCode.INTERNAL_ERROR;
        };
    }

    private static URI requestUri(HttpServletRequest request) {
        return URI.create(request.getRequestURI());
    }

    private static URI requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return requestUri(servletWebRequest.getRequest());
        }
        return URI.create("/");
    }
}
