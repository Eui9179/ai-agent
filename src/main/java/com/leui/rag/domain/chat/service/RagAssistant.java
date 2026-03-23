package com.leui.rag.domain.chat.service;

import dev.langchain4j.service.Result;

public interface RagAssistant {
    Result<String> chat(String userMessage);
}
