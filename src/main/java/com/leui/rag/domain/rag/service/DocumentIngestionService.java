package com.leui.rag.domain.rag.service;

import com.leui.rag.domain.document.parser.DocumentParserFactory;
import com.leui.rag.domain.rag.config.ChunkingProperties;
import com.leui.rag.domain.rag.dto.IngestRequest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChunkingProperties chunkingProperties;

    private final DocumentParserFactory documentParserFactory;

    public int ingest(MultipartFile file, IngestRequest metadata) throws IOException {
        String fileName = file.getOriginalFilename();
        Path tempFile = Files.createTempFile("rag-", fileName);

        try {
            file.transferTo(tempFile);
            log.info("Ingesting document: '{}' ({}bytes)", fileName, file.getSize());

            // LangChain4j: Apache Tika로 파싱
            Document document = FileSystemDocumentLoader.loadDocument(
                    tempFile, documentParserFactory.create(file.getName())
            );

            // 메타데이터 설정 - splitter가 각 TextSegment에 자동 전파
            document.metadata().put("filaId", metadata.fileId());
            document.metadata().put("filaName", metadata.fileName());
            document.metadata().put("client", metadata.fileName());

            // LangChain4j: 재귀적 청킹
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    chunkingProperties.getChunkSize(),
                    chunkingProperties.getChunkOverlap()
            );
            List<TextSegment> segments = splitter.split(document);

            // 임베딩 생성 후 Qdrant 저장
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);

            log.info("Ingestion complete: '{}' → {} chunks", fileName, segments.size());
            return segments.size();

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
