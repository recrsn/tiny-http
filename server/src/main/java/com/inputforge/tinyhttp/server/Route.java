package com.inputforge.tinyhttp.server;

public class Route {
    private final String method;
    private final String path;
    private final RequestHandler handler;

    public Route(String method, String path, RequestHandler handler) {
        this.method = method;
        this.path = path;
        this.handler = handler;
    }

    public boolean matches(Request request) {
        return method.equals(request.method()) && path.equals(request.path());
    }

    public Response handle(Request request) {
        return handler.handle(request);
    }
}
