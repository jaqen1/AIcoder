package com.jaqen.aicoder.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jaqen.aicoder.ai.AiCodeGeneratorService;
import com.jaqen.aicoder.ai.AiCodeGeneratorServiceFactory;
import com.jaqen.aicoder.ai.model.HtmlCodeResult;
import com.jaqen.aicoder.ai.model.MultiFileCodeResult;
import com.jaqen.aicoder.ai.model.message.AiResponseMessage;
import com.jaqen.aicoder.ai.model.message.ToolExecutedMessage;
import com.jaqen.aicoder.ai.model.message.ToolRequestMessage;
import com.jaqen.aicoder.core.parser.CodeParserExecutor;
import com.jaqen.aicoder.core.saver.CodeFileSaverExecutor;
import com.jaqen.aicoder.exception.BusinessException;
import com.jaqen.aicoder.exception.ErrorCode;
import com.jaqen.aicoder.model.entity.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;




    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param appId 应用ID
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);


        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param appId 应用ID
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        // 意图由模型结合历史记忆自行判断；此处原样转发用户消息，不做关键词分流
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, userMessage);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, userMessage);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream);

            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }


    /**
     * 通用流式代码处理方法
     *
     * @param appId 应用ID
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param userMessage 原始用户提示词（用于多文件流式不完整时回退）
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, long appId,
                                           String userMessage) {
        StringBuilder codeBuilder = new StringBuilder();
        int[] chunkCount = {0};
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
            chunkCount[0]++;
        }).doOnError(error -> {
            // #region agent log
            agentLog("C", "AiCodeGeneratorFacade.java:processCodeStream", "stream error",
                    "{\"chunkCount\":" + chunkCount[0] + ",\"builtLen\":" + codeBuilder.length()
                            + ",\"error\":\"" + jsonEscape(String.valueOf(error.getMessage())) + "\"}");
            // #endregion
        }).doOnCancel(() -> {
            // #region agent log
            agentLog("C", "AiCodeGeneratorFacade.java:processCodeStream", "stream cancelled",
                    "{\"chunkCount\":" + chunkCount[0] + ",\"builtLen\":" + codeBuilder.length() + "}");
            // #endregion
        }).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                String lower = completeCode.toLowerCase();
                boolean hasCodeFence = lower.contains("```html") || lower.contains("```css")
                        || lower.contains("```js") || lower.contains("```javascript");
                // #region agent log
                String tail = completeCode.length() <= 240 ? completeCode : completeCode.substring(completeCode.length() - 240);
                agentLog("A", "AiCodeGeneratorFacade.java:processCodeStream", "stream complete",
                        "{\"codeGenType\":\"" + codeGenType + "\",\"chunkCount\":" + chunkCount[0]
                                + ",\"len\":" + completeCode.length()
                                + ",\"hasCodeFence\":" + hasCodeFence
                                + ",\"hasHtmlFence\":" + lower.contains("```html")
                                + ",\"hasCssFence\":" + lower.contains("```css")
                                + ",\"hasJsFence\":" + (lower.contains("```js") || lower.contains("```javascript"))
                                + ",\"fenceHeaders\":\"" + jsonEscape(fenceHeaders(completeCode)) + "\""
                                + ",\"tail\":\"" + jsonEscape(tail) + "\"}");
                // #endregion
                // 以模型实际输出为准：无代码块则视为对话回复，跳过落盘
                if (!hasCodeFence) {
                    log.info("模型未输出代码块，按对话回复跳过保存, appId={}", appId);
                    return;
                }
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 多文件：CSS / JS 分开补全，避免单次输出把 token 花光导致缺文件
                if (codeGenType == CodeGenTypeEnum.MULTI_FILE) {
                    MultiFileCodeResult multiFileResult = (MultiFileCodeResult) parsedResult;
                    // #region agent log
                    agentLog("E", "AiCodeGeneratorFacade.java:processCodeStream", "multi-file completeness check",
                            "{\"htmlBlank\":" + StrUtil.isBlank(multiFileResult.getHtmlCode())
                                    + ",\"cssBlank\":" + StrUtil.isBlank(multiFileResult.getCssCode())
                                    + ",\"jsBlank\":" + StrUtil.isBlank(multiFileResult.getJsCode()) + "}");
                    // #endregion
                    if (StrUtil.isBlank(multiFileResult.getHtmlCode())) {
                        log.info("未解析到 HTML 代码，按对话处理并跳过保存, appId={}", appId);
                        return;
                    }
                    if (StrUtil.isBlank(multiFileResult.getCssCode())) {
                        String cssCode = generateMissingCodePart(appId, userMessage, multiFileResult.getHtmlCode(), "css");
                        if (StrUtil.isNotBlank(cssCode)) {
                            multiFileResult.setCssCode(cssCode);
                        }
                    }
                    if (StrUtil.isBlank(multiFileResult.getJsCode())) {
                        String jsCode = generateMissingCodePart(appId, userMessage, multiFileResult.getHtmlCode(), "javascript");
                        if (StrUtil.isNotBlank(jsCode)) {
                            multiFileResult.setJsCode(jsCode);
                        }
                    }
                    parsedResult = multiFileResult;
                    // #region agent log
                    agentLog("F", "AiCodeGeneratorFacade.java:processCodeStream", "merged after separate css/js follow-up",
                            "{\"cssBlank\":" + StrUtil.isBlank(multiFileResult.getCssCode())
                                    + ",\"jsBlank\":" + StrUtil.isBlank(multiFileResult.getJsCode())
                                    + ",\"cssLen\":" + (multiFileResult.getCssCode() == null ? -1 : multiFileResult.getCssCode().length())
                                    + ",\"jsLen\":" + (multiFileResult.getJsCode() == null ? -1 : multiFileResult.getJsCode().length()) + "}");
                    // #endregion
                }
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                // #region agent log
                agentLog("D", "AiCodeGeneratorFacade.java:processCodeStream", "save failed",
                        "{\"error\":\"" + jsonEscape(e.getClass().getSimpleName() + ": " + e.getMessage()) + "\"}");
                // #endregion
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 单独流式生成缺失的 CSS 或 JavaScript，并解析出对应代码。
     *
     * @param part css 或 javascript
     */
    private String generateMissingCodePart(long appId, String userMessage, String htmlCode, String part) {
        boolean cssPart = "css".equalsIgnoreCase(part);
        String followUpMessage = "原始需求：\n" + userMessage
                + "\n\n已有 index.html：\n```html\n" + htmlCode + "\n```\n\n"
                + (cssPart
                ? "请只输出一个完整的 ```css 代码块（style.css），不要输出 html/javascript。"
                : "请只输出一个完整的 ```javascript 代码块（script.js），不要输出 html/css。");
        log.warn("多文件流式补全 {}, appId={}, htmlLen={}", part, appId, htmlCode.length());
        // 使用无记忆临时实例，避免把 CSS/JS 补全写入应用对话历史
        AiCodeGeneratorService ephemeralService = aiCodeGeneratorServiceFactory.createEphemeralAiCodeGeneratorService();
        String followUpContent = ephemeralService.generateCssJsCodeStream(followUpMessage)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
        // #region agent log
        String followUpLower = followUpContent == null ? "" : followUpContent.toLowerCase();
        agentLog("F", "AiCodeGeneratorFacade.java:generateMissingCodePart", "single-part follow-up",
                "{\"part\":\"" + part + "\",\"appId\":" + appId
                        + ",\"followUpLen\":" + (followUpContent == null ? -1 : followUpContent.length())
                        + ",\"hasCssFence\":" + followUpLower.contains("```css")
                        + ",\"hasJsFence\":" + (followUpLower.contains("```js") || followUpLower.contains("```javascript"))
                        + ",\"fenceHeaders\":\"" + jsonEscape(fenceHeaders(followUpContent == null ? "" : followUpContent)) + "\"}");
        // #endregion
        if (StrUtil.isBlank(followUpContent)) {
            return null;
        }
        MultiFileCodeResult parsed = (MultiFileCodeResult) CodeParserExecutor
                .executeParser(followUpContent, CodeGenTypeEnum.MULTI_FILE);
        return cssPart ? parsed.getCssCode() : parsed.getJsCode();
    }

    // #region agent log
    private static String fenceHeaders(String content) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("```[^\\r\\n]*").matcher(content);
        StringBuilder headers = new StringBuilder();
        int count = 0;
        while (matcher.find() && count < 12) {
            if (count > 0) {
                headers.append(" | ");
            }
            headers.append(matcher.group());
            count++;
        }
        headers.append(" | total=").append(count);
        return headers.toString();
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    private static void agentLog(String hypothesisId, String location, String message, String dataJson) {
        try {
            String line = "{\"sessionId\":\"0fb43b\",\"runId\":\"post-fix\",\"hypothesisId\":\"" + hypothesisId
                    + "\",\"location\":\"" + location + "\",\"message\":\"" + message + "\",\"data\":" + dataJson
                    + ",\"timestamp\":" + System.currentTimeMillis() + "}\n";
            java.nio.file.Files.writeString(java.nio.file.Path.of("e:\\code\\AIcoder\\.cursor\\debug-0fb43b.log"),
                    line, java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }
    // #endregion




}
