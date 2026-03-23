package com.leui.rag.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ChatResponse {
    private String answer;
    private List<Source> sources;

    @AllArgsConstructor
    @Getter
    public static class Source {
        private String fileId;
        private String fileName;
        private int page;
    }
}
