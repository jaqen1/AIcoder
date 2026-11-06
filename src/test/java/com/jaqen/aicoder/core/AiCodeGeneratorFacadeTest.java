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
        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成工作记录小工具，总共不超过30行代码", CodeGenTypeEnum.HTML);
        Assertions.assertNotNull(file);
    }

    @Test
    void generatestream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("生成工作记录小工具,总共不超过30行代码", CodeGenTypeEnum.HTML);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);

        String complete = String.join("", result);
        Assertions.assertNotNull(complete);


    }
}