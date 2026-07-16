package vn.edu.fpt.myschool.auth.infrastructure.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
@EnableConfigurationProperties(SmsGatewayProperties.class)
class SmsGatewayConfiguration {
}
