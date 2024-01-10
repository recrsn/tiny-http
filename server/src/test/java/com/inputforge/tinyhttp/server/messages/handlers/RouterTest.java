package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.*;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouterTest {
    @Test
    void testPathMatching() {
        var router = Router.builder()
                .on("GET", "/", (request) -> Response.noContent())
                .build();

        var response = router.handle(new Request(
                new InetSocketAddress(64166),
                Instant.now(),
                "GET",
                "/",
                "HTTP/1.1",
                HeaderBag.of(),
                null
        ));
        assertEquals(204, response.status().code());
    }

    @Test
    void testNotFound() {
        var router = Router.builder()
                .on("GET", "/", (request) -> Response.noContent())
                .build();

        assertThrows(NotFoundException.class, () ->
                router.handle(new Request(
                        new InetSocketAddress(64166),
                        Instant.now(),
                        "GET",
                        "/not-found",
                        "HTTP/1.1",
                        HeaderBag.of(),
                        null
                )));
    }

    @Test
    void testMethodNotAllowed() {
        var router = Router.builder()
                .on("GET", "/", (request) -> Response.noContent())
                .build();

        assertThrows(MethodNotAllowedException.class, () ->
                router.handle(new Request(
                        new InetSocketAddress(64166),
                        Instant.now(),
                        "POST",
                        "/",
                        "HTTP/1.1",
                        HeaderBag.of(),
                        null
                )));
    }
}