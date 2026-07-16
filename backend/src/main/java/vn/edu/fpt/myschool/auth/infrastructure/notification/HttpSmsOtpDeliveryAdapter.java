package vn.edu.fpt.myschool.auth.infrastructure.notification;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryException;
import vn.edu.fpt.myschool.auth.application.port.OtpDeliveryPort;

@Component
@Profile("prod")
class HttpSmsOtpDeliveryAdapter implements OtpDeliveryPort {

    private static final String PASSWORD_RESET_TEMPLATE = "PASSWORD_RESET_OTP";

    private final RestClient restClient;
    private final SmsGatewayProperties properties;

    HttpSmsOtpDeliveryAdapter(
            RestClient.Builder restClientBuilder,
            SmsGatewayProperties properties) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.apiKey())
                .build();
        this.properties = properties;
    }

    @Override
    public void sendPasswordResetOtp(String phoneNumber, String otp) {
        try {
            restClient.post()
                    .uri(properties.endpoint())
                    .body(new SmsRequest(
                            phoneNumber,
                            PASSWORD_RESET_TEMPLATE,
                            otp,
                            properties.senderId()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new OtpDeliveryException("SMS gateway request failed", exception);
        }
    }

    private record SmsRequest(String to, String template, String code, String senderId) {
    }
}
