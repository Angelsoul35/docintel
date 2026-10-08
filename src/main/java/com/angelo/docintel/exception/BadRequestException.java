package com.angelo.docintel.exception;

/** Thrown when a request is missing required fields; mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
