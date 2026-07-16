package vn.edu.fpt.myschool.auth.application.port;

public interface OtpDeliveryPort {

    void sendPasswordResetOtp(String phoneNumber, String otp);
}
