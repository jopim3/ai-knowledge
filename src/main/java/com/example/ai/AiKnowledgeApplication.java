package com.example.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.ai.mapper")
public class AiKnowledgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiKnowledgeApplication.class, args);
    }
}