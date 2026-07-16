package vn.edu.fpt.myschool.auth.application.port;

import java.time.Duration;
import java.time.Instant;

public interface AdminLoginAttemptStore {

    boolean isBlocked(String identifierHash, Instant now);

    void recordFailure(
            String identifierHash,
            Instant now,
            Duration attemptWindow,
            int maxAttempts,
            Duration blockDuration);

    void clear(String identifierHash);
}
