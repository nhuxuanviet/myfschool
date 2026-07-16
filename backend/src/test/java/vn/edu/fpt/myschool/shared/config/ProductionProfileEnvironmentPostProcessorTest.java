package vn.edu.fpt.myschool.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

class ProductionProfileEnvironmentPostProcessorTest {

    private final ProductionProfileEnvironmentPostProcessor guard =
            new ProductionProfileEnvironmentPostProcessor();

    @ParameterizedTest
    @ValueSource(strings = {"dev", "e2e", "test"})
    void rejectsUnsafeProfilesCombinedWithProduction(String unsafeProfile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod", unsafeProfile);

        assertThatThrownBy(() -> guard.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod")
                .hasMessageContaining(unsafeProfile);
    }

    @Test
    void permitsProductionByItselfAndNonProductionCombinations() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        MockEnvironment nonProduction = new MockEnvironment();
        nonProduction.setActiveProfiles("test", "e2e");

        assertThatCode(() -> guard.postProcessEnvironment(production, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> guard.postProcessEnvironment(nonProduction, null))
                .doesNotThrowAnyException();
    }

    @Test
    void isRegisteredAsAnEnvironmentPostProcessor() throws Exception {
        var factories = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("META-INF/spring.factories"));

        assertThat(factories.getProperty(EnvironmentPostProcessor.class.getName()))
                .contains(ProductionProfileEnvironmentPostProcessor.class.getName());
    }
}
