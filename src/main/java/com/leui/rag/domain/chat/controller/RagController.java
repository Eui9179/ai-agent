package com.leui.rag.domain.chat.controller;

import com.leui.rag.domain.chat.dto.ChatRequest;
import com.leui.rag.domain.chat.dto.ChatResponse;
import com.leui.rag.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "RAG 기반 질의응답 API")
public class RagController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "질의응답", description = "업로드된 문서를 기반으로 LLM이 답변을 생성합니다.")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
