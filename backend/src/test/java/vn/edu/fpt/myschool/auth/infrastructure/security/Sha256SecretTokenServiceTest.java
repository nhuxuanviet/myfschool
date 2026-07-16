package vn.edu.fpt.myschool.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import vn.edu.fpt.myschool.auth.application.AuthProperties;

class Sha256SecretTokenServiceTest {

    private final Sha256SecretTokenService tokenService = new Sha256SecretTokenService(
            new AuthProperties(
                    "test-issuer",
                    "test-only-jwt-signing-secret-not-for-production",
                    Duration.ofMinutes(10),
                    Duration.ofDays(30),
                    Duration.ofMinutes(10),
                    Duration.ofMinutes(10),
                    5,
                    Duration.ofMinutes(15),
                    3,
                    "test-only-otp-hashing-pepper-not-for-production",
                    4));

    @Test
    void generatesOpaqueTokensAndStoresOnlyDeterministicHashes() {
        String firstToken = tokenService.generateToken();
        String secondToken = tokenService.generateToken();

        assertThat(firstToken).hasSize(43).isNotEqualTo(secondToken);
        assertThat(tokenService.hash(firstToken))
                .hasSize(64)
                .doesNotContain(firstToken);
        assertThat(tokenService.matchesHash(firstToken, tokenService.hash(firstToken))).isTrue();
        assertThat(tokenService.matchesHash(secondToken, tokenService.hash(firstToken))).isFalse();
    }

    @Test
    void bindsOtpHashesToTheirChallenge() {
        UUID firstChallenge = UUID.randomUUID();
        UUID secondChallenge = UUID.randomUUID();
        String otpHash = tokenService.hashOtp(firstChallenge, "123456");

        assertThat(tokenService.matchesOtp(firstChallenge, "123456", otpHash)).isTrue();
        assertThat(tokenService.matchesOtp(secondChallenge, "123456", otpHash)).isFalse();
        assertThat(tokenService.matchesOtp(firstChallenge, "654321", otpHash)).isFalse();
    }
}
