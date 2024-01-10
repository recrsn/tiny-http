package com.inputforge.tinyhttp.server.messages;

public class ContentTooLargeException extends ResponseStatusException {
    public ContentTooLargeException() {
        super(HttpStatus.CONTENT_TOO_LARGE, "Request entity too large");
    }
}
