package vn.edu.fpt.myschool.auth.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import vn.edu.fpt.myschool.shared.error.ApiErrorCode;
import vn.edu.fpt.myschool.shared.error.ApiException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        exceptionResolver.resolveException(
                request,
                response,
                null,
                new ApiException(
                        HttpStatus.FORBIDDEN,
                        ApiErrorCode.ACCESS_DENIED.name(),
                        "Access is denied"));
    }
}
