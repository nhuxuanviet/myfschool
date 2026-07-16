package vn.edu.fpt.myschool.auth.infrastructure.security;

import java.security.SecureRandom;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.application.port.OtpGenerator;

@Component
@Profile("!e2e | prod")
class SecureOtpGenerator implements OtpGenerator {

    private static final int OTP_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(secureRandom.nextInt(OTP_BOUND));
    }
}
