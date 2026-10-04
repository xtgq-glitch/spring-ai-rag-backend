package com.example.ragdemo.exception;

/** Invalid client input — mapped to {@code 400 Bad Request}. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
