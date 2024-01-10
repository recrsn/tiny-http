package com.inputforge.tinyhttp.server.messages;

import java.io.IOException;

public class ChunkedEncodingException extends IOException {
    public ChunkedEncodingException(String message) {
        super(message);
    }
}
