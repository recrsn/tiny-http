package com.inputforge.tinyhttp.server.messages.handlers;

public record Route(
        String method,
        PathMatcher matcher,
        RequestHandler handler) {
}
