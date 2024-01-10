package com.inputforge.tinyhttp.server.messages;

public class ResponseStatusException extends RuntimeException {
    private final HttpStatus httpStatus;

    public ResponseStatusException(HttpStatus httpStatus, String message) {
        super(message);

        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
