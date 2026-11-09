package com.jaqen.aicoder.core;

import com.jaqen.aicoder.model.entity.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;


@SpringBootTest
class AiCodeGeneratorFacadeTest {


    @Resource
    AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generate() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成工作记录小工具，总共不超过30行代码", CodeGenTypeEnum.HTML,1);
        Assertions.assertNotNull(file);
    }

    @Test
    void generatestream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("创建炫酷个人博客网站，这是一个现代风格的响应式个人博客网站，包含导航栏、特色文章区、博客文章列表和页脚。网站使用了CSS动画和JavaScript交互来创造炫酷效果。", CodeGenTypeEnum.MULTI_FILE,2);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);

        String complete = String.join("", result);
        Assertions.assertNotNull(complete);


    }
}