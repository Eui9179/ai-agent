package com.leui.rag.domain.document.parser;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DocumentParserFactory {

    private final ApacheTikaDocumentParser tikaParser;

    public DocumentParser create(String ext) {
        return switch (ext) {
            default -> tikaParser;
        };
    }
}
