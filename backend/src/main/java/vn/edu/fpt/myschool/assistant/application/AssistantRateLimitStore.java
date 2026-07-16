package vn.edu.fpt.myschool.assistant.application;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface AssistantRateLimitStore {

    boolean tryAcquire(UUID userId, Instant requestedAt, Duration window, int maxRequests);
}
