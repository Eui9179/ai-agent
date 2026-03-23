package com.leui.rag;

import com.leui.rag.domain.chat.service.RagAssistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class RagApplicationTests {

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@MockitoBean
	private EmbeddingStore<TextSegment> embeddingStore;

	@MockitoBean
	private ChatModel chatModel;

	@MockitoBean
	private ContentRetriever contentRetriever;

	@MockitoBean
	private RagAssistant ragAssistant;

	@Test
	void contextLoads() {
	}

}
