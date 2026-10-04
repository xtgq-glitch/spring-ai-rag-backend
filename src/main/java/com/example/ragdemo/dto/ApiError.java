package com.example.ragdemo.dto;

/** Error payload returned by {@code GlobalExceptionHandler}. */
public record ApiError(int status, String error, String message) {
}
