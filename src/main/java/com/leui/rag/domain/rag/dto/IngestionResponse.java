package com.leui.rag.domain.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class IngestionResponse {
    private String fileName;
    private int chunkCount;
}
