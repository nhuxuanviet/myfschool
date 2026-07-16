package vn.edu.fpt.myschool.assistant.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.edu.fpt.myschool.assistant.application.AssistantEngine;
import vn.edu.fpt.myschool.assistant.application.AssistantRuntimeSettingsStore;
import vn.edu.fpt.myschool.assistant.application.AssistantUsageProperties;
import vn.edu.fpt.myschool.assistant.application.LocalAssistantEngineFactory;
import vn.edu.fpt.myschool.assistant.application.StudentAssistantDataFacade;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AssistantUsageProperties.class)
public class AssistantEngineConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.assistant",
            name = "provider",
            havingValue = "local",
            matchIfMissing = true)
    AssistantEngine localAssistantEngine(LocalAssistantEngineFactory factory) {
        return factory.create();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.assistant", name = "provider", havingValue = "openai")
    AssistantEngine springAiAssistantEngine(
            ChatClient.Builder builder,
            StudentAssistantDataFacade data,
            AssistantRuntimeSettingsStore settingsStore) {
        return new SpringAiAssistantEngine(builder, data, settingsStore);
    }
}
