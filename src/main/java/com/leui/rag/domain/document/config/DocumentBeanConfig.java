package com.leui.rag.domain.document.config;


import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentBeanConfig {

    @Bean
    public ApacheTikaDocumentParser tikaParser() {
        return new ApacheTikaDocumentParser();
    }
}