package vn.edu.fpt.myschool.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void makesConfigurationCollectionsImmutable() {
        CorsProperties properties = new CorsProperties(
                List.of("/api/**"),
                List.of("http://localhost:4173"),
                List.of("GET"),
                List.of("Content-Type"),
                List.of("Location"),
                false,
                Duration.ofMinutes(30));

        assertThatThrownBy(() -> properties.allowedOrigins().add("http://example.com"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(properties.maxAge()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void rejectsWildcardOriginWhenCredentialsAreEnabled() {
        assertThatThrownBy(() -> new CorsProperties(
                List.of("/api/**"),
                List.of("*"),
                List.of("GET"),
                List.of("Content-Type"),
                List.of(),
                true,
                Duration.ofMinutes(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wildcard CORS origins");
    }
}
