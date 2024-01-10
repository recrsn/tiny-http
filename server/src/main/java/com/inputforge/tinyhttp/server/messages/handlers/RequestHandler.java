package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.HttpStatus;
import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;

public interface RequestHandler {
    Response handle(Request request);

    static RequestHandler notFound() {
        return request -> new Response(HttpStatus.NOT_FOUND);
    }
}
