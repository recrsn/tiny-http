package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.*;

import java.util.ArrayList;
import java.util.List;

public class Router implements RequestHandler {
    private final List<Route> routes;

    public Router() {
        this(new ArrayList<>());
    }

    public Router(List<Route> routes) {
        this.routes = routes;
    }

    public void match(String method, String path, RequestHandler handler) {
        routes.add(new Route(method, path, handler));
    }

    @Override
    public Response handle(Request request) {
        for (var route : routes) {
            if (route.matches(request)) {
                return route.handle(request);
            }
        }
        return notFound();
    }

    private Response notFound() {
        return new Response(HttpStatus.NOT_FOUND, HeaderBag.of(), ResponseBody.empty());
    }
}
