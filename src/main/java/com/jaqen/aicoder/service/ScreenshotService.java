package com.jaqen.aicoder.service;

public interface ScreenshotService {

    /**
     *  截图服务，返回访问地址
     *
     * @param webUrl  目标地址
     * @return  截图的访问地址
     */
    String generateAndUploadScreenshot(String webUrl);

}
