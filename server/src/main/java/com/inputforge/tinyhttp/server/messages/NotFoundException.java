package com.inputforge.tinyhttp.server.messages;

public class NotFoundException extends ResponseStatusException {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found");
    }
}
