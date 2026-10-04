package com.example.ragdemo.dto;

/**
 * Response body for {@code POST /api/documents}.
 *
 * @param filename    the uploaded file name
 * @param chunks      number of chunks produced by this upload
 * @param totalChunks total number of chunks currently held in the vector store
 */
public record UploadResponse(String filename, int chunks, int totalChunks) {
}
