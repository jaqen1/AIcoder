package com.jaqen.aicoder.core.saver;

import cn.hutool.core.util.StrUtil;
import com.jaqen.aicoder.ai.model.MultiFileCodeResult;
import com.jaqen.aicoder.exception.BusinessException;
import com.jaqen.aicoder.exception.ErrorCode;
import com.jaqen.aicoder.model.entity.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存器
 *
 * @author jaqen
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // #region agent log
        agentLog("D", "MultiFileCodeFileSaverTemplate.java:saveFiles", "before write",
                "{\"dir\":\"" + jsonEscape(baseDirPath) + "\""
                        + ",\"htmlBlank\":" + cn.hutool.core.util.StrUtil.isBlank(result.getHtmlCode())
                        + ",\"cssBlank\":" + cn.hutool.core.util.StrUtil.isBlank(result.getCssCode())
                        + ",\"jsBlank\":" + cn.hutool.core.util.StrUtil.isBlank(result.getJsCode())
                        + ",\"htmlLen\":" + (result.getHtmlCode() == null ? -1 : result.getHtmlCode().length())
                        + ",\"cssLen\":" + (result.getCssCode() == null ? -1 : result.getCssCode().length())
                        + ",\"jsLen\":" + (result.getJsCode() == null ? -1 : result.getJsCode().length()) + "}");
        // #endregion
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    // #region agent log
    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
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

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
