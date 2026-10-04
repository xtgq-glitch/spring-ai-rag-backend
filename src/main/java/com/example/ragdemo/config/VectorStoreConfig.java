package com.example.ragdemo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory vector store ({@link SimpleVectorStore}).
 *
 * <p>Contents are kept in a {@code ConcurrentHashMap} and are therefore lost when the
 * process restarts — swap in a persistent store (PGVector, Redis, ...) for production use.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
