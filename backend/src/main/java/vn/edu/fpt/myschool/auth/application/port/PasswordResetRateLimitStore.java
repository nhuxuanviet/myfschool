package vn.edu.fpt.myschool.auth.application.port;

import java.time.Duration;
import java.time.Instant;

public interface PasswordResetRateLimitStore {

    boolean tryAcquire(
            String phoneHash,
            Instant requestedAt,
            Duration window,
            int maxRequests);
}
