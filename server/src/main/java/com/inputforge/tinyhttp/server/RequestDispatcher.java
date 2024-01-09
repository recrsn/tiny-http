package com.inputforge.tinyhttp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RequestDispatcher {
    private final RequestHandler requestHandler;
    private final ResponseWriter responseWriter;
    Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);

    public RequestDispatcher(RequestHandler requestHandler, ResponseWriter responseWriter) {
        this.requestHandler = requestHandler;
        this.responseWriter = responseWriter;
    }

    public void handle(Socket socket) throws IOException {
        logger.info("Handling connection from {}", socket.getRemoteSocketAddress());

        socket.setKeepAlive(true);
        socket.setSoTimeout(5000);

        var inputStream = socket.getInputStream();
        var outputStream = socket.getOutputStream();

        var requestIterator = new RequestParser((InetSocketAddress) socket.getRemoteSocketAddress(), inputStream);

        for (var optionalRequest = requestIterator.next(); optionalRequest.isPresent(); optionalRequest = requestIterator.next()) {
            var request = optionalRequest.get();
            var response = requestHandler.handle(request);
            responseWriter.write(response, outputStream);
        }

        // Keep-alive connection closed
        socket.close();
        logger.info("Connection to {} closed", socket.getRemoteSocketAddress());

    }
}
