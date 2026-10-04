package com.example.ragdemo.dto;

import java.util.List;

/**
 * Response body for {@code POST /api/chat}.
 *
 * @param answer  the generated answer
 * @param sources the retrieved context snippets the answer was grounded in
 */
public record ChatResponse(String answer, List<String> sources) {
}
