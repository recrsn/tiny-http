package com.inputforge.tinyhttp.server;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServerBuilder {

    private InetAddress listenAddress = InetAddress.getLoopbackAddress();
    private int port = 8080;
    private RequestDispatcher requestDispatcher;
    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public HttpServerBuilder listenAddress(InetAddress address) {
        this.listenAddress = address;
        return this;
    }

    public HttpServerBuilder port(int port) {
        this.port = port;
        return this;
    }


    public HttpServerBuilder with(RequestHandler handler) {
        this.requestDispatcher = new RequestDispatcher(handler, new ResponseWriter());
        return this;
    }

    public HttpServer build() {
        return new HttpServer(listenAddress, port, requestDispatcher, executorService);
    }
}
