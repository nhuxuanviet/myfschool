package vn.edu.fpt.myschool.auth.infrastructure.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.application.port.OtpGenerator;

@Component
@Profile("e2e & !prod")
class FixedE2eOtpGenerator implements OtpGenerator {

    @Override
    public String generate() {
        return "123456";
    }
}
