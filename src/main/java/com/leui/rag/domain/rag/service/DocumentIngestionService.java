package com.leui.rag.domain.rag.service;

import com.leui.rag.domain.rag.config.ChunkingProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    // Spring AI 추상화 인터페이스에 의존 (Qdrant 구현체에 직접 의존 X)
    private final VectorStore vectorStore;
    private final ChunkingProperties chunkingProperties;

    public int ingest(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        Path tempFile = Files.createTempFile("leui-rag-", "-" + fileName);

        try {
            file.transferTo(tempFile);
            log.info("Ingesting document: '{}' ({}bytes)", fileName, file.getSize());

            // LangChain4j: Apache Tika로 파싱 (PDF, Word, txt 등 지원 포맷)
            Document document = FileSystemDocumentLoader.loadDocument(
                    tempFile, new ApacheTikaDocumentParser()
            );

            // LangChain4j: 재귀적 청킹 - 설정값은 application.properties에서 관리
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    chunkingProperties.getChunkSize(),
                    chunkingProperties.getChunkOverlap()
            );
            List<TextSegment> segments = splitter.split(document);

            // Spring AI Document로 변환 - 메타데이터(source, timestamp) 보존
            String ingestedAt = Instant.now().toString();
            List<org.springframework.ai.document.Document> springDocs = segments.stream()
                    .map(seg -> new org.springframework.ai.document.Document(
                            seg.text(),
                            Map.of(
                                    "source", fileName,
                                    "ingested_at", ingestedAt
                            )
                    ))
                    .toList();

            vectorStore.add(springDocs);
            log.info("Ingestion complete: '{}' → {} chunks", fileName, springDocs.size());
            return springDocs.size();

        } finally {
            // 임시 파일 반드시 삭제 (try-finally 보장)
            Files.deleteIfExists(tempFile);
            log.debug("Temp file deleted: {}", tempFile);
        }
    }
}
