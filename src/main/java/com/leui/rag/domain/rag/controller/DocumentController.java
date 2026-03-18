package com.leui.rag.domain.rag.controller;

import com.leui.rag.domain.rag.dto.IngestRequest;
import com.leui.rag.domain.rag.dto.IngestionResponse;
import com.leui.rag.domain.rag.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/v1/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping("/ingest")
    public IngestionResponse ingest(
            @RequestParam("file") MultipartFile file,
            @RequestPart("metadata") IngestRequest metadata
    ) throws IOException {
        int chunkCount = documentIngestionService.ingest(file, metadata);
        return new IngestionResponse(file.getOriginalFilename(), chunkCount);
    }
}
