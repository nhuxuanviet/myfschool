package vn.edu.fpt.myschool.assistant.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.assistant.application.AssistantRateLimitStore;

@Repository
class PostgresAssistantRateLimitStore implements AssistantRateLimitStore {

    private static final String ACQUIRE_PERMIT_SQL = """
            INSERT INTO assistant_rate_limits (
                user_id, window_started_at, request_count, updated_at
            )
            VALUES (?, ?, 1, ?)
            ON CONFLICT (user_id) DO UPDATE
            SET window_started_at = CASE
                    WHEN assistant_rate_limits.window_started_at <= ? THEN EXCLUDED.window_started_at
                    ELSE assistant_rate_limits.window_started_at
                END,
                request_count = CASE
                    WHEN assistant_rate_limits.window_started_at <= ? THEN 1
                    ELSE assistant_rate_limits.request_count + 1
                END,
                updated_at = EXCLUDED.updated_at
            WHERE assistant_rate_limits.window_started_at <= ?
               OR assistant_rate_limits.request_count < ?
            RETURNING request_count
            """;

    private final JdbcTemplate jdbcTemplate;

    PostgresAssistantRateLimitStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(
            UUID userId,
            Instant requestedAt,
            Duration window,
            int maxRequests) {
        Timestamp requestedTimestamp = Timestamp.from(requestedAt);
        Timestamp cutoffTimestamp = Timestamp.from(requestedAt.minus(window));
        return !jdbcTemplate.queryForList(
                        ACQUIRE_PERMIT_SQL,
                        Integer.class,
                        userId,
                        requestedTimestamp,
                        requestedTimestamp,
                        cutoffTimestamp,
                        cutoffTimestamp,
                        cutoffTimestamp,
                        maxRequests)
                .isEmpty();
    }
}
