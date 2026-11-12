package com.jaqen.aicoder;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class test {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Test
    void contextLoads() {
        System.out.println("baseUrl: " + baseUrl);
        System.out.println("apiKey: " + apiKey);
        System.out.println("modelName: " + modelName);
    }
}
