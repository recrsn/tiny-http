package com.inputforge.tinyhttp.server.messages;

public class MethodNotAllowedException extends ResponseStatusException {
    public MethodNotAllowedException() {
        super(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
    }
}
