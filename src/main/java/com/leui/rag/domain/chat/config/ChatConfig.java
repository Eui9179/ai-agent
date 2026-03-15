package com.leui.rag.domain.chat.config;

import com.leui.rag.domain.rag.config.ChunkingProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChunkingProperties.class)
public class ChatConfig {

    // ChatClient.Builder는 Spring AI가 자동 구성 (OllamaChatModel에 직접 의존하지 않음)
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
