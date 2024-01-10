package com.inputforge.tinyhttp.server.messages;

public class RequestEntityTooLargeException extends ResponseStatusException {
    public RequestEntityTooLargeException() {
        super(HttpStatus.REQUEST_ENTITY_TOO_LARGE, "Request entity too large");
    }
}
