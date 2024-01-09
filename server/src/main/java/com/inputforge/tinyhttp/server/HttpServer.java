package com.inputforge.tinyhttp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class HttpServer {
    private final InetAddress listenAddress;
    private final int port;
    private final RequestDispatcher requestDispatcher;
    private final ExecutorService executorService;
    Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private volatile boolean running = false;

    public HttpServer(InetAddress listenAddress, int port, RequestDispatcher requestDispatcher, ExecutorService executorService) {
        this.listenAddress = listenAddress;
        this.port = port;
        this.requestDispatcher = requestDispatcher;
        this.executorService = executorService;
    }

    public static HttpServerBuilder builder() {
        return new HttpServerBuilder();
    }

    public void start() throws IOException {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port, 0, listenAddress)) {
            logger.info("Listening on {}:{}", listenAddress, port);
            while (running) {
                Socket socket = serverSocket.accept();
                logger.info("Incoming connection from {}", socket.getRemoteSocketAddress());
                this.executorService.submit(() -> {
                    try {
                        requestDispatcher.handle(socket);
                    } catch (IOException e) {
                        logger.error("Error handling request", e);
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
    }

    public void stop() {
        running = false;
        executorService.shutdown();
    }
}
