package com.jaqen.aicoder.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {

    /**
     * 下载 源码 服务
     *
     * @param projectPath       文件路径
     * @param downloadFileName  文件名字
     * @param response          返回前端 响应
     * @return
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
