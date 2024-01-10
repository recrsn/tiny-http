package com.inputforge.tinyhttp.server.messages;

import com.inputforge.tinyhttp.server.messages.HttpStatus;
import com.inputforge.tinyhttp.server.messages.ResponseStatusException;

public class NotFoundException extends ResponseStatusException {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found");
    }
}
