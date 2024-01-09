package com.inputforge.tinyhttp.server;

public class BadRequestException extends ResponseStatusException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
