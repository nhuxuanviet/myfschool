package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.myschool.auth.application.port.SecurityAuditStore;

@Repository
class JdbcSecurityAuditStore implements SecurityAuditStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcSecurityAuditStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(
            String eventType,
            UUID userId,
            String identifierHash,
            Instant occurredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO security_audit_events (
                    id, user_id, event_type, identifier_hash, occurred_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                userId,
                eventType,
                identifierHash,
                Timestamp.from(occurredAt));
    }
}
