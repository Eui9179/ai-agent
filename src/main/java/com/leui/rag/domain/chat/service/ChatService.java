package com.leui.rag.domain.chat.service;

import com.leui.rag.domain.chat.dto.ChatRequest;
import com.leui.rag.domain.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RagAssistant ragAssistant;

    public ChatResponse chat(ChatRequest request) {
        log.info("RAG chat request: '{}'", request.getMessage());
        String answer = ragAssistant.chat(request.getMessage());
        log.info("RAG chat response generated");
        return new ChatResponse(answer);
    }
}
