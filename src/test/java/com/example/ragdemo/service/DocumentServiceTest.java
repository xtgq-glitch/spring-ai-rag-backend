package com.example.ragdemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import com.example.ragdemo.dto.UploadResponse;
import com.example.ragdemo.exception.InvalidRequestException;
import com.example.ragdemo.exception.UnsupportedDocumentTypeException;

/** Upload validation and the parse -> chunk -> store pipeline (vector store is mocked). */
class DocumentServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);

    private final DocumentService documentService = new DocumentService(vectorStore);

    @Test
    void emptyFileIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentService.ingest(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty");

        verifyNoInteractions(vectorStore);
    }

    @Test
    void unsupportedFileTypeIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "script.sh", "text/plain",
                "echo hi".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> documentService.ingest(file))
                .isInstanceOf(UnsupportedDocumentTypeException.class);

        verifyNoInteractions(vectorStore);
    }

    @Test
    void supportedFileIsChunkedAndStored() {
        MockMultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain",
                "The hotel budget for first-tier cities is CNY 600 per night.".getBytes(StandardCharsets.UTF_8));

        UploadResponse response = documentService.ingest(file);

        assertThat(response.filename()).isEqualTo("policy.txt");
        assertThat(response.chunks()).isPositive();
        assertThat(response.totalChunks()).isEqualTo(response.chunks());
        verify(vectorStore).add(anyList());
    }
}
