package com.leui.rag.domain.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {
    private String host;
    private int port;
    private String collectionName;
}
