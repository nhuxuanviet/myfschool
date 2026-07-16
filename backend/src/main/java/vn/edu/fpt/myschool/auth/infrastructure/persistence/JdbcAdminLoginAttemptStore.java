package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.AdminLoginAttemptStore;

@Repository
class JdbcAdminLoginAttemptStore implements AdminLoginAttemptStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcAdminLoginAttemptStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isBlocked(String identifierHash, Instant now) {
        return Boolean.TRUE.equals(jdbcTemplate.query(
                """
                SELECT blocked_until
                FROM admin_login_attempts
                WHERE identifier_hash = ?
                """,
                resultSet -> resultSet.next()
                        && readInstant(resultSet, "blocked_until")
                                .map(blockedUntil -> blockedUntil.isAfter(now))
                                .orElse(false),
                identifierHash));
    }

    @Override
    public void recordFailure(
            String identifierHash,
            Instant now,
            Duration attemptWindow,
            int maxAttempts,
            Duration blockDuration) {
        Optional<AttemptState> existing = findForUpdate(identifierHash);
        if (existing.isEmpty()) {
            try {
                insert(identifierHash, now, maxAttempts, blockDuration);
                return;
            } catch (DuplicateKeyException ignored) {
                existing = findForUpdate(identifierHash);
            }
        }

        AttemptState current = existing.orElseThrow();
        boolean windowExpired = !current.windowStartedAt().plus(attemptWindow).isAfter(now);
        int nextCount = windowExpired ? 1 : current.attemptCount() + 1;
        Instant windowStartedAt = windowExpired ? now : current.windowStartedAt();
        Instant blockedUntil = nextCount >= maxAttempts ? now.plus(blockDuration) : null;
        jdbcTemplate.update(
                """
                UPDATE admin_login_attempts
                SET window_started_at = ?, attempt_count = ?, blocked_until = ?, updated_at = ?
                WHERE identifier_hash = ?
                """,
                Timestamp.from(windowStartedAt),
                nextCount,
                toTimestamp(blockedUntil),
                Timestamp.from(now),
                identifierHash);
    }

    @Override
    public void clear(String identifierHash) {
        jdbcTemplate.update(
                "DELETE FROM admin_login_attempts WHERE identifier_hash = ?",
                identifierHash);
    }

    private Optional<AttemptState> findForUpdate(String identifierHash) {
        return jdbcTemplate.query(
                        """
                        SELECT window_started_at, attempt_count
                        FROM admin_login_attempts
                        WHERE identifier_hash = ?
                        FOR UPDATE
                        """,
                        (resultSet, rowNumber) -> toAttemptState(resultSet),
                        identifierHash)
                .stream()
                .findFirst();
    }

    private void insert(
            String identifierHash,
            Instant now,
            int maxAttempts,
            Duration blockDuration) {
        Instant blockedUntil = maxAttempts == 1 ? now.plus(blockDuration) : null;
        jdbcTemplate.update(
                """
                INSERT INTO admin_login_attempts (
                    identifier_hash, window_started_at, attempt_count, blocked_until, updated_at
                ) VALUES (?, ?, 1, ?, ?)
                """,
                identifierHash,
                Timestamp.from(now),
                toTimestamp(blockedUntil),
                Timestamp.from(now));
    }

    private static AttemptState toAttemptState(ResultSet resultSet) throws SQLException {
        return new AttemptState(
                readInstant(resultSet, "window_started_at").orElseThrow(),
                resultSet.getInt("attempt_count"));
    }

    private static Optional<Instant> readInstant(ResultSet resultSet, String column)
            throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return Optional.ofNullable(value).map(OffsetDateTime::toInstant);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private record AttemptState(Instant windowStartedAt, int attemptCount) {
    }
}
