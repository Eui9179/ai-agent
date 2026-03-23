package com.leui.rag.domain.rag.service;

import com.leui.rag.domain.rag.config.ChunkingProperties;
import com.leui.rag.domain.rag.dto.IngestRequest;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChunkingProperties chunkingProperties;

    @Value("${upload.dir}")
    private String uploadDir;

    public int ingest(MultipartFile file, IngestRequest metadata) throws IOException {
        String fileName = file.getOriginalFilename();
        Path tempFile = Files.createTempFile("rag-", fileName);

        try {
            file.transferTo(tempFile);
            log.info("Ingesting document: '{}' ({}bytes)", fileName, file.getSize());

            // 원본 PDF 저장 (다운로드용)
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Files.copy(tempFile, uploadPath.resolve(metadata.fileId() + ".pdf"), StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved original PDF: {}/{}.pdf", uploadDir, metadata.fileId());

            List<Document> documents = loadPdfByPage(tempFile, metadata);

            DocumentSplitter splitter = DocumentSplitters.recursive(
                    chunkingProperties.getChunkSize(),
                    chunkingProperties.getChunkOverlap()
            );

            List<TextSegment> segments = new ArrayList<>();
            for (Document document : documents) {
                segments.addAll(splitter.split(document));
            }

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);

            log.info("Ingestion complete: '{}' → {} chunks", fileName, segments.size());
            return segments.size();

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * PDF를 페이지 단위로 파싱해 각 Document에 page 메타데이터를 부여합니다.
     * 이후 청킹 시 페이지 번호가 각 TextSegment에 자동으로 전파됩니다.
     */
    private List<Document> loadPdfByPage(Path pdfPath, IngestRequest metadata) throws IOException {
        List<Document> documents = new ArrayList<>();

        try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdf.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf);

                if (text.isBlank()) continue;

                Metadata docMetadata = new Metadata();
                docMetadata.put("fileId", metadata.fileId());
                docMetadata.put("fileName", metadata.fileName());
                docMetadata.put("client", metadata.fileName());
                docMetadata.put("page", String.valueOf(page));
                docMetadata.put("page_total", String.valueOf(totalPages));

                documents.add(Document.from(text, docMetadata));
            }
        }

        return documents;
    }
}
