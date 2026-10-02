package com.example.ragdemo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class RagController {

    private static final String SYSTEM_PROMPT = """
            你是一个严谨的文档问答助手，请只根据下面的参考资料回答用户问题。
            如果参考资料中没有相关内容，请直接回答：文档中没有相关内容。

            === 参考资料 ===
            """;

    private static final int TOP_K = 4;

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    public RagController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /** 上传 PDF：解析 -> 切块 -> 向量化 -> 存入内存向量库 */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam MultipartFile file) throws IOException {
        List<Document> chunks = new TokenTextSplitter()
                .apply(new TikaDocumentReader(new ByteArrayResource(file.getBytes())).read());
        vectorStore.add(chunks);
        return Map.of("filename", String.valueOf(file.getOriginalFilename()), "chunks", chunks.size());
    }

    /** 问答：检索相关片段 -> 拼 Prompt -> DeepSeek 回答 */
    @GetMapping("/chat")
    public Map<String, String> chat(@RequestParam String message) {
        String context = vectorStore
                .similaritySearch(SearchRequest.builder().query(message).topK(TOP_K).build())
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String answer = chatClient.prompt()
                .messages(new SystemMessage(SYSTEM_PROMPT + context), new UserMessage(message))
                .call()
                .content();

        return Map.of("answer", Objects.requireNonNullElse(answer, ""));
    }
}
