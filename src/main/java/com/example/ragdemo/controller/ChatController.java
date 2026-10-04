package com.example.ragdemo.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ragdemo.dto.ChatRequest;
import com.example.ragdemo.dto.ChatResponse;
import com.example.ragdemo.service.RagService;

/** Question-answering API over the indexed documents. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    /** Ask a question; the answer is grounded in the previously uploaded documents. */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return ragService.answer(request.message());
    }
}
