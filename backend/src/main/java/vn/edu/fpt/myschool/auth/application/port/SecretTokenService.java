package vn.edu.fpt.myschool.auth.application.port;

import java.util.UUID;

public interface SecretTokenService {

    String generateToken();

    String hash(String value);

    String hashOtp(UUID challengeId, String otp);

    boolean matchesHash(String value, String expectedHash);

    boolean matchesOtp(UUID challengeId, String otp, String expectedHash);
}
