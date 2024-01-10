package com.inputforge.tinyhttp.server;

import com.inputforge.tinyhttp.server.messages.BadRequestException;
import com.inputforge.tinyhttp.server.messages.Response;
import com.inputforge.tinyhttp.server.messages.ResponseBody;
import com.inputforge.tinyhttp.server.messages.handlers.FileSystemRequestHandler;
import com.inputforge.tinyhttp.server.messages.handlers.LoggingRequestHandler;
import com.inputforge.tinyhttp.server.messages.handlers.Router;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import static com.inputforge.tinyhttp.server.messages.handlers.PathMatcher.prefix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class HttpServerTest {
    private static int port;
    private static HttpServer server;

    private static Path tmpDir;

    @BeforeAll
    static void beforeAll() throws InterruptedException, IOException {
        tmpDir = Files.createTempDirectory("tinyhttp-test");
        Files.writeString(tmpDir.resolve("index.html"), "<html><body>Hello World!</body></html>");
        Files.writeString(tmpDir.resolve("test.txt"), "Hello World!");
        var subdir = tmpDir.resolve("./subdir");
        Files.createDirectories(subdir);
        Files.writeString(subdir.resolve("index.html"), "<html><body>Hello Subdir!</body></html>");

        var router = Router.builder()
                .on("GET", "/huge-response", (request) -> {
                    var body = new byte[1024 * 1024];
                    Arrays.fill(body, (byte) 'a');
                    return new Response(ResponseBody.of(body));
                })
                .on("GET", "/", (request) -> new Response(ResponseBody.of("Hello World!")))
                .on("GET", "/exception", (request) -> {
                    throw new BadRequestException("Test exception");
                })
                .on("GET", "/unknown-exception", (request) -> {
                    throw new RuntimeException("Test exception");
                })
                .on("GET", "/stream", (request) -> new Response(ResponseBody.streaming((out) -> {
                    try (out) {
                        out.write("Hello Streaming World!".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })))
                .on("POST", "/echo", (request) -> new Response(ResponseBody.of(request.body())))
                .on("GET", prefix("/files"), new FileSystemRequestHandler("/files", tmpDir))
                .on("GET", "/pattern/*/test", (request) -> new Response(ResponseBody.of("Hello World!")))
                .on("*", "/any-method", (request) -> new Response(ResponseBody.of("Hello World!")))
                .build();

        server = HttpServer.builder()
                .at(InetAddress.getLoopbackAddress())
                .port(0) // Random port
                .with(LoggingRequestHandler.wrap(router))
                .build();

        new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }).start();
        Thread.sleep(100);
        port = server.port();
    }

    @AfterAll
    static void afterAll() throws IOException {
        server.stop();
        try (var files = Files.walk(tmpDir)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

    }

    @Test
    void testServerDefaultConfiguration() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port()))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello World!", response.body());
        }
    }

    @Test
    void testIncompleteHttpRequest() throws IOException {
        try (
                var socket = new Socket(InetAddress.getLoopbackAddress(), port);
                var outputStream = socket.getOutputStream();
        ) {
            outputStream.write("GET / HTTP\r\n".getBytes());
            outputStream.write("Host: localhost\r\n".getBytes());
            outputStream.flush();
            socket.shutdownOutput();
            var response = new String(socket.getInputStream().readAllBytes()).split("\r\n")[0];
            assertEquals("HTTP/1.1 400 Bad Request", response);
        }
    }

    @Test
    void testVeryLargeHttpRequest() throws IOException {
        try (
                var socket = new Socket(InetAddress.getLoopbackAddress(), port);
                var outputStream = socket.getOutputStream()
        ) {
            outputStream.write("GET / HTTP/1.1\r\n".getBytes());
            outputStream.write("Host: localhost\r\n".getBytes());

            var header = "X-Custom-Header: ";
            var buffer = Arrays.copyOf(header.getBytes(), 10 * 1024);

            Arrays.fill(buffer, header.length(), buffer.length, (byte) 'a');

            outputStream.write(buffer);
            outputStream.write("\r\n".getBytes());
            outputStream.flush();
            socket.shutdownOutput();

            var response = new String(socket.getInputStream().readAllBytes()).split("\r\n")[0];
            assertEquals("HTTP/1.1 413 Content Too Large", response);
        }
    }

    @Test
    void testInvalidHeader() throws IOException {
        try (
                var socket = new Socket(InetAddress.getLoopbackAddress(), port);
                var outputStream = socket.getOutputStream()
        ) {
            socket.getOutputStream().write("GET / HTTP/1.1\r\n".getBytes());
            outputStream.write("Host: localhost\r\n".getBytes());
            outputStream.write("XCustomHeaderaaaaaaa\r\r".getBytes());
            outputStream.write("\r\n".getBytes());
            outputStream.flush();
            socket.shutdownOutput();
            var response = new String(socket.getInputStream().readAllBytes()).split("\r\n")[0];
            assertEquals("HTTP/1.1 400 Bad Request", response);
        }
    }

    @Test
    @Timeout(5)
    void testSurviveMalformedRequests() throws IOException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            try (
                    var socket = new Socket(InetAddress.getLoopbackAddress(), port);
                    var outputStream = socket.getOutputStream()
            ) {
                socket.getOutputStream().write("GET / HTTP/1.1\r\n".getBytes());
                outputStream.write("Host: localhost\r\n".getBytes());
                outputStream.write("XCustomHeaderaaaaaaa".getBytes());
            }
        }

        for (int i = 0; i < 5; i++) {
            try (
                    var socket = new Socket(InetAddress.getLoopbackAddress(), port);
                    var outputStream = socket.getOutputStream()
            ) {
//                socket.shutdownInput();
                outputStream.write(
                        "GET /huge-response HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
                socket.shutdownOutput();
            }
        }

        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port()))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello World!", response.body());
        }
    }

    @Test
    void testExceptionHandling() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/exception"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode());

            response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/unknown-exception"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
        }
    }

    @Test
    void testStreamingResponse() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/stream"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello Streaming World!", response.body());
        }
    }

    @Test
    void testPostEcho() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/echo"))
                    .POST(HttpRequest.BodyPublishers.ofString("Hello World!"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello World!", response.body());
        }
    }

    @Test
    void testFileSystemRequestHandler() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/files/"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("<html><body>Hello World!</body></html>", response.body());
            assertEquals("text/html", response.headers().firstValue("Content-Type").get());
            assertEquals("38", response.headers().firstValue("Content-Length").get());

            response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/files/test.txt"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello World!", response.body());
            assertEquals("text/plain", response.headers().firstValue("Content-Type").get());
            assertEquals("12", response.headers().firstValue("Content-Length").get());

            response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/files/subdir/"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("<html><body>Hello Subdir!</body></html>", response.body());
            assertEquals("text/html", response.headers().firstValue("Content-Type").get());
            assertEquals("39", response.headers().firstValue("Content-Length").get());

            response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/files/does-not-exist"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode());
        }
    }

    @Test
    void testHugeResponse() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/huge-response"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals(1024 * 1024, response.body().length());
        }
    }

    @Test
    void testNotFound() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/not-found"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode());
        }
    }

    @Test
    void testMethodNotAllowed() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/"))
                    .POST(HttpRequest.BodyPublishers.ofString("Hello World!"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(405, response.statusCode());
        }
    }

    @Test
    void testPattern() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/pattern/123/test"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello World!", response.body());
        }
    }

    @Test
    void testAnyMethod() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/any-method"))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals("Hello World!", response.body());

            response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + server.port() + "/any-method"))
                    .POST(HttpRequest.BodyPublishers.ofString("Hello World!"))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
        }
    }
}