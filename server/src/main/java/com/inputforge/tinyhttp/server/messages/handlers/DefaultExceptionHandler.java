package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.HttpStatus;
import com.inputforge.tinyhttp.server.messages.Response;
import com.inputforge.tinyhttp.server.messages.ResponseStatusException;

public class DefaultExceptionHandler implements ExceptionHandler {
    @Override
    public Response handle(Exception exception) {
        if (exception instanceof ResponseStatusException e) {
            return new Response(e.httpStatus());
        }
        return new Response(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
