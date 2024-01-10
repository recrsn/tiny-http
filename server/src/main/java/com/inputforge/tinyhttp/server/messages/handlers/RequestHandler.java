package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;

public interface RequestHandler {
    static RequestHandler notFound() {
        return request -> Response.notFound();
    }

    Response handle(Request request);
}
