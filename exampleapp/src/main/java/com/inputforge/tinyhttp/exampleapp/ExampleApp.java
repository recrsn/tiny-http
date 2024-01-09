package com.inputforge.tinyhttp.exampleapp;

import com.inputforge.tinyhttp.server.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class ExampleApp {
    public static void main(String[] args) throws IOException {
        var router = new Router();

        router.match("GET", "/", request -> new Response(
                HttpStatus.OK, HeaderBag.of(), ResponseBody.of("Hello World!")
        ));

        var server = HttpServer.builder()
                .listenAddress(InetAddress.getLoopbackAddress())
                .port(8080)
                .with(LoggingRequestHandler.wrap(router))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}
