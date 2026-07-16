package vn.edu.fpt.myschool.shared.config;

import java.util.Arrays;
import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public final class ProductionProfileEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    private static final String PRODUCTION_PROFILE = "prod";
    private static final Set<String> UNSAFE_PRODUCTION_PROFILES =
            Set.of("dev", "e2e", "test");

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        Set<String> activeProfiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        if (!activeProfiles.contains(PRODUCTION_PROFILE)) {
            return;
        }

        activeProfiles.stream()
                .filter(UNSAFE_PRODUCTION_PROFILES::contains)
                .findFirst()
                .ifPresent(profile -> {
                    throw new IllegalStateException(
                            "The prod profile cannot be combined with the %s profile"
                                    .formatted(profile));
                });
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
