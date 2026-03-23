package com.leui.rag.domain.rag.controller;

import com.leui.rag.domain.rag.dto.IngestRequest;
import com.leui.rag.domain.rag.dto.IngestionResponse;
import com.leui.rag.domain.rag.service.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/v1/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "문서 수집(Ingest) API")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "문서 업로드", description = "PDF, Word, txt 등의 문서를 파싱·청킹 후 VectorDB에 저장합니다.")
    public IngestionResponse ingest(
            @RequestPart("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("client") String client,
            @RequestParam("fileId") String fileId
    ) throws IOException {
        int chunkCount = documentIngestionService.ingest(file, new IngestRequest(fileName, client, fileId));
        return new IngestionResponse(file.getOriginalFilename(), chunkCount);
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "문서 다운로드", description = "fileId에 해당하는 원본 PDF를 다운로드합니다.")
    public ResponseEntity<Resource> download(@PathVariable String fileId) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(fileId + ".pdf");
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
