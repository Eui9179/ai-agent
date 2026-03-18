package com.leui.rag.domain.rag.dto;

public record IngestRequest(
        String fileName,
        String client,
        String fileId
) {
}
