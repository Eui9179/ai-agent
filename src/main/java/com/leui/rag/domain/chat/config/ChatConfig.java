package com.leui.rag.domain.chat.config;

import com.leui.rag.domain.chat.service.RagAssistant;
import com.leui.rag.domain.rag.config.ChunkingProperties;
import com.leui.rag.domain.rag.config.QdrantProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ChunkingProperties.class, QdrantProperties.class})
public class ChatConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(QdrantProperties props) {
        return QdrantEmbeddingStore.builder()
                .host(props.getHost())
                .port(props.getPort())
                .collectionName(props.getCollectionName())
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .build();
    }

    @Bean
    public RagAssistant ragAssistant(ChatModel chatModel, ContentRetriever contentRetriever) {
        return AiServices.builder(RagAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();
    }
}
