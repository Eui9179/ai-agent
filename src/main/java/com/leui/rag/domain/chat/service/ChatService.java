package com.leui.rag.domain.chat.service;

import com.leui.rag.domain.chat.dto.ChatRequest;
import com.leui.rag.domain.chat.dto.ChatResponse;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RagAssistant ragAssistant;

    public ChatResponse chat(ChatRequest request) {
        log.info("RAG chat request: '{}'", request.getMessage());

        Result<String> result = ragAssistant.chat(request.getMessage());

        List<ChatResponse.Source> sources = result.sources().stream()
                .map(content -> content.textSegment().metadata())
                .map(meta -> new ChatResponse.Source(
                        meta.getString("fileId"),
                        meta.getString("fileName"),
                        parsePageNumber(meta.getString("page"))
                ))
                .filter(s -> s.getFileName() != null)
                .distinct()
                .toList();

        log.info("RAG chat response generated — {} source(s)", sources.size());
        return new ChatResponse(result.content(), sources);
    }

    private int parsePageNumber(String page) {
        try { return Integer.parseInt(page); } catch (Exception e) { return 0; }
    }
}
