package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.PasswordResetRateLimitStore;

@Repository
class PostgresPasswordResetRateLimitStore implements PasswordResetRateLimitStore {

    private static final String ACQUIRE_PERMIT_SQL = """
            INSERT INTO password_reset_rate_limits (
                phone_hash, window_started_at, request_count, updated_at
            )
            VALUES (?, ?, 1, ?)
            ON CONFLICT (phone_hash) DO UPDATE
            SET window_started_at = CASE
                    WHEN password_reset_rate_limits.window_started_at <= ? THEN EXCLUDED.window_started_at
                    ELSE password_reset_rate_limits.window_started_at
                END,
                request_count = CASE
                    WHEN password_reset_rate_limits.window_started_at <= ? THEN 1
                    ELSE password_reset_rate_limits.request_count + 1
                END,
                updated_at = EXCLUDED.updated_at
            WHERE password_reset_rate_limits.window_started_at <= ?
               OR password_reset_rate_limits.request_count < ?
            RETURNING request_count
            """;

    private final JdbcTemplate jdbcTemplate;

    PostgresPasswordResetRateLimitStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(
            String phoneHash,
            Instant requestedAt,
            Duration window,
            int maxRequests) {
        Instant windowCutoff = requestedAt.minus(window);
        Timestamp requestedTimestamp = Timestamp.from(requestedAt);
        Timestamp cutoffTimestamp = Timestamp.from(windowCutoff);
        return !jdbcTemplate.queryForList(
                        ACQUIRE_PERMIT_SQL,
                        Integer.class,
                        phoneHash,
                        requestedTimestamp,
                        requestedTimestamp,
                        cutoffTimestamp,
                        cutoffTimestamp,
                        cutoffTimestamp,
                        maxRequests)
                .isEmpty();
    }
}
