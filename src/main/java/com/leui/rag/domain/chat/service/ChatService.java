package com.leui.rag.domain.chat.service;

import com.leui.rag.domain.chat.dto.ChatRequest;
import com.leui.rag.domain.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatResponse chat(ChatRequest request) {
        log.info("RAG chat request: '{}'", request.getMessage());

        String answer = chatClient.prompt()
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user(request.getMessage())
                .call()
                .content();

        log.info("RAG chat response generated");
        return new ChatResponse(answer);
    }
}
