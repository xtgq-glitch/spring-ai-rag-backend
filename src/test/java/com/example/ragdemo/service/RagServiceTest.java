package com.example.ragdemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.example.ragdemo.dto.ChatResponse;

/** RAG behaviour when the vector store holds no matching context. */
class RagServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);

    private final RagService ragService;

    RagServiceTest() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        given(builder.build()).willReturn(chatClient);
        this.ragService = new RagService(builder, vectorStore);
    }

    @Test
    void emptyVectorStoreReturnsNoContextAnswerInsteadOfFailing() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        ChatResponse response = ragService.answer("What is the hotel budget?");

        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).isEmpty();
    }
}
