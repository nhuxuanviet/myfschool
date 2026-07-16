package vn.edu.fpt.myschool.auth.infrastructure.persistence;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("(dev | e2e) & !prod")
@EnableConfigurationProperties({AuthSeedProperties.class, AdminSeedProperties.class})
class AuthSeedConfiguration {
}
