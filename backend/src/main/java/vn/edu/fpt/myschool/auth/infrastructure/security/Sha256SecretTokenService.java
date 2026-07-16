package vn.edu.fpt.myschool.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.application.AuthProperties;
import vn.edu.fpt.myschool.auth.application.port.SecretTokenService;

@Component
public class Sha256SecretTokenService implements SecretTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String otpPepper;

    public Sha256SecretTokenService(AuthProperties properties) {
        this.otpPepper = properties.otpPepper();
    }

    @Override
    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    @Override
    public String hash(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    @Override
    public String hashOtp(UUID challengeId, String otp) {
        return hash(challengeId + ":" + otp + ":" + otpPepper);
    }

    @Override
    public boolean matchesHash(String value, String expectedHash) {
        return constantTimeEquals(hash(value), expectedHash);
    }

    @Override
    public boolean matchesOtp(UUID challengeId, String otp, String expectedHash) {
        return constantTimeEquals(hashOtp(challengeId, otp), expectedHash);
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static boolean constantTimeEquals(String actual, String expected) {
        if (expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII));
    }
}
