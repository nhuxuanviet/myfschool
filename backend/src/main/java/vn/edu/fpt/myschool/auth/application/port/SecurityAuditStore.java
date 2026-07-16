package vn.edu.fpt.myschool.auth.application.port;

import java.time.Instant;
import java.util.UUID;

public interface SecurityAuditStore {

    void record(
            String eventType,
            UUID userId,
            String identifierHash,
            Instant occurredAt);
}
