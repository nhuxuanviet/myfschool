package vn.edu.fpt.myschool.auth.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class SmsGatewayPropertiesTest {

    @Test
    void acceptsAnHttpsEndpointAndPositiveTimeouts() {
        assertThatCode(() -> properties(
                        URI.create("https://sms.example.test/send"),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInsecureEndpointsAndNonPositiveTimeouts() {
        assertThatThrownBy(() -> properties(
                        URI.create("http://sms.example.test/send"),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
        assertThatThrownBy(() -> properties(
                        URI.create("https://sms.example.test/send"),
                        Duration.ZERO,
                        Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectTimeout");
        assertThatThrownBy(() -> properties(
                        URI.create("https://sms.example.test/send"),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readTimeout");
    }

    private static SmsGatewayProperties properties(
            URI endpoint,
            Duration connectTimeout,
            Duration readTimeout) {
        return new SmsGatewayProperties(
                endpoint,
                "test-api-key",
                "MySchool",
                connectTimeout,
                readTimeout);
    }
}
