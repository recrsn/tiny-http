package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.Response;
import com.inputforge.tinyhttp.server.messages.ResponseStatusException;

public class DefaultExceptionHandler implements ExceptionHandler {
    @Override
    public Response handle(Exception exception) {
        if (exception instanceof ResponseStatusException e) {
            return new Response(e.httpStatus());
        }
        return Response.internalServerError();
    }
}
