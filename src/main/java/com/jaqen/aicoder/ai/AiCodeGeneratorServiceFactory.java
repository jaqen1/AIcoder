package com.jaqen.aicoder.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jaqen.aicoder.ai.tools.FileWriteTool;
import com.jaqen.aicoder.exception.BusinessException;
import com.jaqen.aicoder.exception.ErrorCode;
import com.jaqen.aicoder.model.entity.enums.CodeGenTypeEnum;
import com.jaqen.aicoder.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;


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
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务（带缓存）
     * @param appId  应用ID
     * @param codeGenType  应用类型
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 根据 appId 创建 隔离的  AI 服务实例
     *  应用类型 选择 不通实例，区分 调用模型
     * @param appId  应用ID
     * @param codeGenType  应用类型
     * @return
     */
    public AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        //根据 appid 创建记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();

        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        return switch (codeGenType) {
            case VUE_PROJECT ->
                // VUE 项目 生成， 使用工具调用 和 推理模型
                AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(buildChatModel())
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(new FileWriteTool())
                        // 处理 工具 幻觉问题
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "error: there is no tools called "+ toolExecutionRequest.name()))
                        .build();

            case HTML , MULTI_FILE ->
                // 普通类型 使用 流式 对话模型
                 AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(buildChatModel())
                        .streamingChatModel(buildStreamingChatModel())
                        .chatMemory(chatMemory)
                        .build();


            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持的代码生成类型"+codeGenType.getValue());

        };

    }

    /**
     * 无记忆的临时服务，用于 CSS/JS 完善缺失生成的 辅助调用，避免污染应用对话记忆。
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
                //.returnThinking(false)
              //  .reasoningEffort("none")
              //  .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .build();
    }

    private OpenAiStreamingChatModel buildStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(streamingMaxTokens)
                //.returnThinking(false)
              //  .reasoningEffort("none")
              //  .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .build();
    }

    /**
     *  构建  缓存键
     * @param appId  应用ID
     * @param codeGenType  应用类型
     * @return
     */
    public String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "-" + codeGenType.name();
    }



    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return getAiCodeGeneratorService(0);
    }



}
