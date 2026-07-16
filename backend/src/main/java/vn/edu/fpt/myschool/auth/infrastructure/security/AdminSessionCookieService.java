package vn.edu.fpt.myschool.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.application.AdminSessionException;
import vn.edu.fpt.myschool.auth.application.AdminSessionProperties;
import vn.edu.fpt.myschool.auth.application.AuthProperties;
import vn.edu.fpt.myschool.auth.application.port.SecretTokenService;

@Component
public class AdminSessionCookieService {

    private static final String SAME_SITE = "Strict";
    private static final String COOKIE_PATH = "/";

    private final AdminSessionProperties properties;
    private final AuthProperties authProperties;
    private final SecretTokenService secretTokenService;

    public AdminSessionCookieService(
            AdminSessionProperties properties,
            AuthProperties authProperties,
            SecretTokenService secretTokenService) {
        this.properties = properties;
        this.authProperties = authProperties;
        this.secretTokenService = secretTokenService;
    }

    public String issue(
            HttpServletResponse response,
            String refreshToken) {
        String csrfToken = secretTokenService.generateToken();
        addCookie(
                response,
                properties.refreshCookieName(),
                refreshToken,
                true,
                authProperties.refreshTokenTtl());
        addCookie(
                response,
                properties.csrfCookieName(),
                csrfToken,
                false,
                authProperties.refreshTokenTtl());
        return csrfToken;
    }

    public String requireRefreshToken(HttpServletRequest request) {
        return findCookie(request, properties.refreshCookieName())
                .filter(value -> !value.isBlank())
                .orElseThrow(AdminSessionException::missingRefreshToken);
    }

    public void requireValidCsrf(HttpServletRequest request) {
        String cookieToken = findCookie(request, properties.csrfCookieName())
                .orElseThrow(AdminSessionException::invalidCsrfToken);
        String headerToken = request.getHeader(properties.csrfHeaderName());
        if (headerToken == null || !constantTimeEquals(cookieToken, headerToken)) {
            throw AdminSessionException.invalidCsrfToken();
        }
    }

    public void clear(HttpServletResponse response) {
        addCookie(
                response,
                properties.refreshCookieName(),
                "",
                true,
                Duration.ZERO);
        addCookie(
                response,
                properties.csrfCookieName(),
                "",
                false,
                Duration.ZERO);
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            boolean httpOnly,
            Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(properties.secureCookies())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static java.util.Optional<String> findCookie(
            HttpServletRequest request,
            String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
