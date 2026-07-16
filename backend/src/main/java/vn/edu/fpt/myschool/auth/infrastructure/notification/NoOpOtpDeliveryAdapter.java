package vn.edu.fpt.myschool.auth.infrastructure.notification;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryPort;

@Component
@Profile("!prod")
public class NoOpOtpDeliveryAdapter implements OtpDeliveryPort {

    @Override
    public void sendPasswordResetOtp(String phoneNumber, String otp) {
        // SMS provider integration is intentionally kept behind this port.
    }
}
