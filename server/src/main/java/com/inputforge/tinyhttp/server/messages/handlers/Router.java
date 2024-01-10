package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.MethodNotAllowedException;
import com.inputforge.tinyhttp.server.messages.NotFoundException;
import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;

import java.util.List;

public abstract class Router implements RequestHandler {
    public static RouterBuilder builder() {
        return new RouterBuilder();
    }

    public abstract List<Route> findCandidateRoutes(String path);

    @Override
    public Response handle(Request request) {
        boolean hasPathMatch = false;
        for (var route : findCandidateRoutes(request.path())) {
            var matcher = route.matcher();
            if (matcher.matches(request.path())) {
                hasPathMatch = true;
                if (methodMatches(route, request)) {
                    return route.handler().handle(request);
                }
            }
        }
        if (hasPathMatch) {
            throw new MethodNotAllowedException();
        }
        throw new NotFoundException();
    }

    private boolean methodMatches(Route route, Request request) {
        if ("*".equals(route.method())) {
            return true;
        }

        return route.method().equalsIgnoreCase(request.method());
    }
}
