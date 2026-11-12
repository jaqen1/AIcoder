package com.jaqen.aicoder.core.parser;

import com.jaqen.aicoder.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * @author jaqen
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile(
            "```html\\s*\\r?\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile(
            "```css\\s*\\r?\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile(
            "```(?:js|javascript)\\s*\\r?\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    // 流式截断时可能缺少结束围栏，允许匹配到文本末尾
    private static final Pattern HTML_CODE_OPEN_PATTERN = Pattern.compile(
            "```html\\s*\\r?\\n([\\s\\S]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_OPEN_PATTERN = Pattern.compile(
            "```css\\s*\\r?\\n([\\s\\S]*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_OPEN_PATTERN = Pattern.compile(
            "```(?:js|javascript)\\s*\\r?\\n([\\s\\S]*)$", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        String htmlCode = extractCode(codeContent, HTML_CODE_PATTERN, HTML_CODE_OPEN_PATTERN);
        String cssCode = extractCode(codeContent, CSS_CODE_PATTERN, CSS_CODE_OPEN_PATTERN);
        String jsCode = extractCode(codeContent, JS_CODE_PATTERN, JS_CODE_OPEN_PATTERN);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        // #region agent log
        agentLog("B", "MultiFileCodeParser.java:parseCode", "parsed lengths",
                "{\"contentLen\":" + (codeContent == null ? 0 : codeContent.length())
                        + ",\"htmlLen\":" + (htmlCode == null ? -1 : htmlCode.length())
                        + ",\"cssLen\":" + (cssCode == null ? -1 : cssCode.length())
                        + ",\"jsLen\":" + (jsCode == null ? -1 : jsCode.length()) + "}");
        // #endregion
        return result;
    }

    // #region agent log
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

    /**
     * 先按闭合围栏提取；若失败再按“开放到文末”提取（兼容流式截断）。
     */
    private String extractCode(String content, Pattern closedPattern, Pattern openPattern) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        Matcher closed = closedPattern.matcher(content);
        if (closed.find()) {
            return closed.group(1);
        }
        Matcher open = openPattern.matcher(content);
        if (open.find()) {
            return open.group(1);
        }
        return null;
    }
}
