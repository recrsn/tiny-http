package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.HeaderBag;
import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;
import com.inputforge.tinyhttp.server.messages.ResponseBody;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggingRequestHandlerTest {
    @Test
    void testLogRequest() {
        var router = Router.builder()
        .on("GET", "/", (request) -> new Response(ResponseBody.of("Hello World!")))
        .on("GET", "/stream", (request) -> new Response(ResponseBody.streaming((out) -> {
            try {
                out.write("Hello Streaming World!".getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }))).build();

        var out = new ByteArrayOutputStream();
        var logStream = new PrintStream(out);
        var handler = LoggingRequestHandler.wrap(logStream, router);
        var timestamp = ZonedDateTime.of(
                2024, 1, 10, 18, 30, 0, 0,
                ZoneOffset.UTC
        ).toInstant();
        handler.handle(new Request(
                new InetSocketAddress(64166),
                timestamp,
                "GET",
                "/",
                "HTTP/1.1",
                HeaderBag.of(),
                null
        ));
        handler.handle(new Request(
                new InetSocketAddress(64166),
                timestamp,
                "GET",
                "/stream",
                "HTTP/1.1",
                HeaderBag.of(),
                null
        ));

        assertEquals("""
                0.0.0.0 - - [10/Jan/2024:18:30:00 +0000] "GET / HTTP/1.1" 200 12
                0.0.0.0 - - [10/Jan/2024:18:30:00 +0000] "GET /stream HTTP/1.1" 200 -
                """, out.toString());
    }

}