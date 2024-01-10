package com.inputforge.tinyhttp.exampleapp;

import com.inputforge.tinyhttp.server.HttpServer;
import com.inputforge.tinyhttp.server.messages.HeaderBag;
import com.inputforge.tinyhttp.server.messages.HttpStatus;
import com.inputforge.tinyhttp.server.messages.Response;
import com.inputforge.tinyhttp.server.messages.ResponseBody;
import com.inputforge.tinyhttp.server.messages.handlers.LoggingRequestHandler;
import com.inputforge.tinyhttp.server.messages.handlers.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class ExampleApp {

    private static final Logger logger = LoggerFactory.getLogger(ExampleApp.class);

    public static void main(String[] args) throws IOException {
        var router = Router.builder()
                .on("GET", "/", request -> new Response(
                        HttpStatus.OK, HeaderBag.of(), ResponseBody.of("Hello World!")
                )).on("POST", "/hello", request -> {
                    try (var inputStream = request.body()) {
                        var name = new String(inputStream.readAllBytes());
                        return new Response(
                                HttpStatus.OK, HeaderBag.of(),
                                ResponseBody.of("Hello " + name + "!")
                        );
                    } catch (IOException e) {
                        logger.error("Error reading request body", e);
                        return new Response(
                                HttpStatus.INTERNAL_SERVER_ERROR, HeaderBag.of(),
                                ResponseBody.of("Error reading request body")
                        );
                    }
                }).build();

        var server = HttpServer.builder()
                .at(InetAddress.getLoopbackAddress())
                .port(8080)
                .with(LoggingRequestHandler.wrap(router))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}
