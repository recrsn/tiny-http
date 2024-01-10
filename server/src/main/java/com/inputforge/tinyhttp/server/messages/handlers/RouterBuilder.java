package com.inputforge.tinyhttp.server.messages.handlers;

import java.util.ArrayList;
import java.util.List;

import static com.inputforge.tinyhttp.server.messages.handlers.PathMatcher.path;

public class RouterBuilder {
    private final List<Route> routes = new ArrayList<>();

    public RouterBuilder on(String method, String path, RequestHandler handler) {
        routes.add(new Route(method, path(path), handler));
        return this;
    }

    public RouterBuilder on(String method, PathMatcher matcher, RequestHandler handler) {
        routes.add(new Route(method, matcher, handler));
        return this;
    }

    public Router build() {
        return new PrefixTreeRouter(routes);
    }
}
