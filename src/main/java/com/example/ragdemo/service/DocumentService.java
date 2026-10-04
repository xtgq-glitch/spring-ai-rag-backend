package com.example.ragdemo.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ragdemo.dto.UploadResponse;
import com.example.ragdemo.exception.InvalidRequestException;
import com.example.ragdemo.exception.UnsupportedDocumentTypeException;

/**
 * Document ingestion pipeline: parse (Apache Tika) -> split into token chunks ->
 * embed -> store in the vector store.
 */
@Service
public class DocumentService {

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("pdf", "txt", "md", "doc", "docx", "ppt", "pptx", "html", "htm");

    private final VectorStore vectorStore;

    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    private final AtomicInteger totalChunks = new AtomicInteger();

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public UploadResponse ingest(MultipartFile file) {
        validate(file);
        List<Document> chunks = parse(file);
        if (chunks.isEmpty()) {
            throw new InvalidRequestException("No text content could be extracted from the uploaded file");
        }
        vectorStore.add(chunks);
        return new UploadResponse(filename(file), chunks.size(), totalChunks.addAndGet(chunks.size()));
    }

    /** Number of chunks currently indexed (in-memory store, reset on restart). */
    public int indexedChunks() {
        return totalChunks.get();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file is empty");
        }
        String name = filename(file).toLowerCase(Locale.ROOT);
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedDocumentTypeException(
                    "Unsupported file type '." + extension + "'. Supported types: " + SUPPORTED_EXTENSIONS);
        }
    }

    private List<Document> parse(MultipartFile file) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes());
            return textSplitter.apply(new TikaDocumentReader(resource).read());
        }
        catch (IOException ex) {
            throw new InvalidRequestException("Failed to read the uploaded file: " + ex.getMessage());
        }
        catch (RuntimeException ex) {
            throw new InvalidRequestException("Failed to parse the uploaded document: " + ex.getMessage());
        }
    }

    private String filename(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name == null || name.isBlank()) ? "unnamed" : name;
    }
}
