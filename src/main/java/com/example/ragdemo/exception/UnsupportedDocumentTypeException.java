package com.example.ragdemo.exception;

/** Unsupported document format — mapped to {@code 415 Unsupported Media Type}. */
public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
}
