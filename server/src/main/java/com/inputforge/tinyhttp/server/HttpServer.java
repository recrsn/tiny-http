package com.inputforge.tinyhttp.server;

import com.inputforge.tinyhttp.server.messages.HttpConnection;
import com.inputforge.tinyhttp.server.messages.Request;
import com.inputforge.tinyhttp.server.messages.Response;
import com.inputforge.tinyhttp.server.messages.ResponseStatusException;
import com.inputforge.tinyhttp.server.messages.handlers.ExceptionHandler;
import com.inputforge.tinyhttp.server.messages.handlers.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final InetAddress listenAddress;
    private final ExecutorService executorService;
    private final ExceptionHandler exceptionHandler;
    private final RequestHandler requestHandler;
    private int port;
    private volatile boolean running = false;

    public HttpServer(InetAddress listenAddress, int port, ExecutorService executorService, ExceptionHandler exceptionHandler, RequestHandler requestHandler) {
        this.listenAddress = listenAddress;
        this.port = port;
        this.executorService = executorService;
        this.exceptionHandler = exceptionHandler;
        this.requestHandler = requestHandler;
    }

    public static HttpServerBuilder builder() {
        return new HttpServerBuilder();
    }

    public void start() throws IOException {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port, 0, listenAddress)) {
            port = serverSocket.getLocalPort();
            logger.info("Listening on {}:{}", listenAddress, port);
            while (running) {
                Socket socket = serverSocket.accept();
                logger.info("Incoming connection from {}", socket.getRemoteSocketAddress());
                this.executorService.submit(() -> {
                    try (socket) {
                        handleConnection(socket);
                        logger.info("Closing connection from {}", socket.getRemoteSocketAddress());
                    } catch (IOException e) {
                        logger.error("Error handling request", e);
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
    }

    private void handleConnection(Socket socket) throws IOException {
        try (var connection = new HttpConnection(socket)) {
            while (true) {
                Optional<Request> nextRequest;
                try {
                    nextRequest = connection.nextRequest();
                } catch (Exception e) {
                    // Treat any protocol-parsing error as unrecoverable
                    // and close the connection
                    logger.error("Error while parsing request", e);
                    var response = exceptionHandler.handle(e);
                    connection.sendResponse(response);
                    break;
                }

                if (nextRequest.isEmpty()) {
                    // No more requests, close the connection
                    break;
                }

                var request = nextRequest.get();
                var response = handleRequest(request);
                connection.sendResponse(response);
                drainRequest(request);
            }
        }
    }

    private Response handleRequest(Request request) {
        try {
            return requestHandler.handle(request);
        } catch (ResponseStatusException e) {
            return exceptionHandler.handle(request, e);
        } catch (Exception e) {
            logger.error("Unexpected error while handling request", e);
            return exceptionHandler.handle(e);
        }
    }

    private void drainRequest(Request request) throws IOException {
        try (var inputStream = request.body()) {
            while (inputStream.read() != -1) {
                // Drain the input stream, do nothing
            }
        }
    }

    public void stop() {
        running = false;
        executorService.shutdown();
    }

    public int port() {
        return port;
    }
}
