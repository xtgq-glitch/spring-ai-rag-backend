package com.example.ragdemo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.example.ragdemo.dto.ChatResponse;

/**
 * Retrieval-augmented generation: similarity search over the indexed chunks, then
 * answer the question with the LLM, grounded in the retrieved context.
 */
@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
            You are a document question-answering assistant.
            Answer the user's question using ONLY the reference material below.
            If the reference material does not contain the answer, reply exactly:
            "The uploaded documents do not contain this information."
            Always answer in the same language as the user's question.

            === REFERENCE MATERIAL ===
            """;

    private static final String NO_CONTEXT_ANSWER =
            "No relevant content was found in the uploaded documents.";

    private static final int TOP_K = 4;

    private static final int SNIPPET_MAX_LENGTH = 300;

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public ChatResponse answer(String question) {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(TOP_K).build());

        if (hits.isEmpty()) {
            return new ChatResponse(NO_CONTEXT_ANSWER, List.of());
        }

        String context = hits.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String answer = chatClient.prompt()
                .messages(new SystemMessage(SYSTEM_PROMPT + context), new UserMessage(question))
                .call()
                .content();

        List<String> sources = hits.stream().map(document -> snippet(document.getText())).toList();
        return new ChatResponse(answer == null ? "" : answer, sources);
    }

    private String snippet(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= SNIPPET_MAX_LENGTH
                ? normalized
                : normalized.substring(0, SNIPPET_MAX_LENGTH) + "...";
    }
}
