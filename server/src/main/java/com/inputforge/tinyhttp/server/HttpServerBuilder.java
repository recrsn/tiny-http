package com.inputforge.tinyhttp.server;

import com.inputforge.tinyhttp.server.messages.handlers.DefaultExceptionHandler;
import com.inputforge.tinyhttp.server.messages.handlers.ExceptionHandler;
import com.inputforge.tinyhttp.server.messages.handlers.RequestHandler;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServerBuilder {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final ExceptionHandler exceptionHandler = new DefaultExceptionHandler();
    private InetAddress listenAddress = InetAddress.getLoopbackAddress();
    private int port = 8080;
    private RequestHandler requestHandler = RequestHandler.notFound();


    public HttpServerBuilder at(InetAddress address) {
        this.listenAddress = address;
        return this;
    }

    public HttpServerBuilder port(int port) {
        this.port = port;
        return this;
    }


    public HttpServerBuilder with(RequestHandler handler) {
        this.requestHandler = handler;
        return this;
    }


    public HttpServer build() {
        return new HttpServer(
                listenAddress,
                port,
                executorService,
                exceptionHandler,
                requestHandler
        );
    }
}
