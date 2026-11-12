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
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。。", CodeGenTypeEnum.MULTI_FILE,6);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);

        String complete = String.join("", result);
        Assertions.assertNotNull(complete);


    }
}