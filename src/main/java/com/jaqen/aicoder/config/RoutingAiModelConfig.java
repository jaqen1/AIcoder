package com.jaqen.aicoder.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.routing-chat-model")
@Data
public class RoutingAiModelConfig {



    private String baseUrl;

    private String apiKey;

    private String modelName;
    /**
     * 创建 多实例的  用于路由判断的ChatModel
     *
     * 选择 阿里模型
     *
     */
    @Bean
    @Scope("prototype")
    public ChatModel routingChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.1)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
