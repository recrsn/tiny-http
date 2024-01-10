package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;

import java.io.PrintStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class LoggingRequestHandler implements RequestHandler {
    private final PrintStream logger;
    private final RequestHandler handler;

    public LoggingRequestHandler(PrintStream logger, RequestHandler handler) {
        this.logger = logger;
        this.handler = handler;
    }

    public static RequestHandler wrap(RequestHandler router) {
        return new LoggingRequestHandler(System.out, router);
    }

    @Override
    public Response handle(Request request) {
        var response = handler.handle(request);
        var contentLength = response.body().getContentLength();
        logger.printf("%s - - [%s] \"%s %s %s\" %d %s%n",
                request.remoteAddress().getAddress().getHostAddress(),
                request.timestamp().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(
                        "dd/MMM/yyyy:HH:mm:ss Z")
                ),
                request.method(),
                request.path(),
                request.version(),
                response.status().code(),
                contentLength == -1 ? '-' : String.valueOf(contentLength));
        return response;
    }
}
