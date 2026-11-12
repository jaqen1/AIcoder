package com.jaqen.aicoder.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jaqen.aicoder.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens:8192}")
    private Integer streamingMaxTokens;


    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 根据 appId 创建 隔离的  AI 服务实例
     *
      * @param appId
     * @return
     */
    public AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        //根据 appid 创建记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();

        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(buildChatModel())
                .streamingChatModel(buildStreamingChatModel())
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 无记忆的临时服务，用于 CSS/JS 补全等辅助调用，避免污染应用对话记忆。
     */
    public AiCodeGeneratorService createEphemeralAiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(buildChatModel())
                .streamingChatModel(buildStreamingChatModel())
                .build();
    }

    private ChatModel buildChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .returnThinking(false)
                .reasoningEffort("none")
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .build();
    }

    private OpenAiStreamingChatModel buildStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(streamingMaxTokens)
                .returnThinking(false)
                .reasoningEffort("none")
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .build();
    }

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return getAiCodeGeneratorService(0);
    }



}
