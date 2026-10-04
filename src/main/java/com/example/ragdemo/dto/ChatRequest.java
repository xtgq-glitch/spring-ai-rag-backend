package com.example.ragdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/chat}. */
public record ChatRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 2000, message = "must be at most 2000 characters")
        String message) {
}
