package com.inputforge.tinyhttp.server;

public interface RequestHandler {
    Response handle(Request request);
}
