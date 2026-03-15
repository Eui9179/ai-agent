package com.leui.rag.domain.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "rag.chunking")
public class ChunkingProperties {
    private int chunkSize = 500;
    private int chunkOverlap = 50;
}
