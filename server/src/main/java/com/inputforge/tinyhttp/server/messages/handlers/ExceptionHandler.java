package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;

import java.util.Optional;

public interface ExceptionHandler {
    Response handle(Exception exception);

    default Response handle(Request request, Exception exception) {
        return handle(exception);
    }
}
