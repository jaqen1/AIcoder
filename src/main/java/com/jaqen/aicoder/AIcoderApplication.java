package com.jaqen.aicoder;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class} )
@MapperScan("com.jaqen.aicoder.mapper")
public class AIcoderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIcoderApplication.class, args);
    }

}
