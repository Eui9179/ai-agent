package com.leui.rag.domain.rag.controller;

import com.leui.rag.domain.rag.dto.IngestionResponse;
import com.leui.rag.domain.rag.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    @PostMapping("/ingest")
    public IngestionResponse ingest(@RequestParam("file") MultipartFile file) throws IOException {
        int chunkCount = documentIngestionService.ingest(file);
        return new IngestionResponse(file.getOriginalFilename(), chunkCount);
    }

}
