package vn.edu.fpt.myschool.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfiguration implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebConfiguration(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        for (String pathPattern : corsProperties.pathPatterns()) {
            registry.addMapping(pathPattern)
                    .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                    .allowedMethods(corsProperties.allowedMethods().toArray(String[]::new))
                    .allowedHeaders(corsProperties.allowedHeaders().toArray(String[]::new))
                    .exposedHeaders(corsProperties.exposedHeaders().toArray(String[]::new))
                    .allowCredentials(corsProperties.allowCredentials())
                    .maxAge(corsProperties.maxAge().toSeconds());
        }
    }
}
