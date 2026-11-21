package com.jaqen.aicoder.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 代码生成类型路由专用 ChatModel（阿里云百炼 OpenAI 兼容模式）
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.routing-chat-model")
@Data
@Deprecated
public class RoutingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    /**
     *  框架 直接注入的 实例，因 考虑 项目并发，弃用
     * @return
     */
    @Bean
    public ChatModel routingChatModelSingle() {
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
