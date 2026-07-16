package vn.edu.fpt.myschool.auth.application;

import java.util.UUID;

public record ResetChallengeResult(UUID challengeId, long expiresIn) {
}
