package com.leui.rag.domain.rag.controller;

import com.leui.rag.domain.rag.dto.IngestRequest;
import com.leui.rag.domain.rag.dto.IngestionResponse;
import com.leui.rag.domain.rag.service.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/v1/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "문서 수집(Ingest) API")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping("/ingest")
    @Operation(summary = "문서 업로드", description = "PDF, Word, txt 등의 문서를 파싱·청킹 후 VectorDB에 저장합니다.")
    public IngestionResponse ingest(
            @RequestParam("file") MultipartFile file,
            @RequestPart("metadata") IngestRequest metadata
    ) throws IOException {
        int chunkCount = documentIngestionService.ingest(file, metadata);
        return new IngestionResponse(file.getOriginalFilename(), chunkCount);
    }
}
