package com.example.ragdemo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.example.ragdemo.controller.ChatController;
import com.example.ragdemo.controller.DocumentController;
import com.example.ragdemo.dto.ChatResponse;
import com.example.ragdemo.exception.InvalidRequestException;
import com.example.ragdemo.exception.UnsupportedDocumentTypeException;
import com.example.ragdemo.service.DocumentService;
import com.example.ragdemo.service.RagService;

/** Verifies that each failure mode is mapped to the right HTTP status and error payload. */
@WebMvcTest({ ChatController.class, DocumentController.class })
class ApiErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void blankChatMessageIsRejected() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("message must not be blank"));
    }

    @Test
    void nullChatMessageIsRejected() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validChatMessageIsAnswered() throws Exception {
        given(ragService.answer("What is the hotel budget?"))
                .willReturn(new ChatResponse("CNY 600 per night.", List.of("Hotel budget: CNY 600")));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is the hotel budget?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("CNY 600 per night."));
    }

    @Test
    void emptyUploadIsRejected() throws Exception {
        given(documentService.ingest(any()))
                .willThrow(new InvalidRequestException("Uploaded file is empty"));

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));
    }

    @Test
    void unsupportedDocumentTypeIsRejected() throws Exception {
        given(documentService.ingest(any()))
                .willThrow(new UnsupportedDocumentTypeException("Unsupported file type '.sh'"));

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "script.sh", "text/plain", "echo hi".getBytes())))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void oversizedUploadIsRejected() throws Exception {
        given(documentService.ingest(any())).willThrow(new MaxUploadSizeExceededException(1024));

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[2048])))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingInternals() throws Exception {
        given(ragService.answer(any()))
                .willThrow(new IllegalStateException("jdbc:postgresql://internal-host:5432/secret refused"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"anything\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
