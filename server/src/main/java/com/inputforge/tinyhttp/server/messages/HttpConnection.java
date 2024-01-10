package com.inputforge.tinyhttp.server.messages;

import com.inputforge.tinyhttp.server.messages.io.ChunkedInputStream;
import com.inputforge.tinyhttp.server.messages.io.ChunkedOutputStream;
import com.inputforge.tinyhttp.server.messages.io.LimitedInputStream;
import com.inputforge.tinyhttp.server.messages.io.UncloseableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpConnection implements Closeable {
    // Headers must be less than 8KB
    public static final int MAX_HEADER_SIZE = 8 * 1024;
    private static final HeaderBag DEFAULT_HEADERS = HeaderBag.of(Map.of(
            "Server", "TinyHttp"
    ));
    private final Socket socket;
    private final InputStream inputStream;
    private final byte[] buffer = new byte[MAX_HEADER_SIZE];
    private final InetSocketAddress clientAddress;
    private final BufferedOutputStream outputStream;
    Logger logger = LoggerFactory.getLogger(HttpConnection.class);
    private int requestCount = 0;

    public HttpConnection(Socket socket) throws IOException {
        this.socket = socket;

        socket.setKeepAlive(true);
        socket.setSoTimeout(5000);

        this.clientAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        this.inputStream = new BufferedInputStream(socket.getInputStream());
        this.outputStream = new BufferedOutputStream(socket.getOutputStream());
    }

    private String[] parseRequestLine(String requestLine) {
        if (requestLine.isEmpty()) {
            throw new BadRequestException("Invalid status line");
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new BadRequestException("Invalid status line");
        }

        if (!parts[2].startsWith("HTTP/")) {
            throw new BadRequestException("Invalid status line");
        }
        return parts;
    }

    public Optional<Request> nextRequest() {
        // Limit the number of requests per connection
        if (requestCount > 1000) {
            return Optional.empty();
        }

        try {
            Instant timestamp = Instant.now();
            StringBuilder stringBuilder = new StringBuilder();
            int readIndex;

            drainCRLF();

            inputStream.mark(buffer.length + 1);
            int read = inputStream.read(buffer);

            if (read == -1) {
                return Optional.empty();
            }

            ParseState state = ParseState.STATUS;

            String method = null;
            String path = null;
            String version = null;

            for (readIndex = 0; readIndex < read; readIndex++) {
                char c = (char) buffer[readIndex];

                if (state == ParseState.CR && c != '\n') {
                    throw new BadRequestException("Invalid header");
                } else if (c == '\r') {
                    state = ParseState.CR;
                } else if (c == '\n') {
                    state = ParseState.HEADER;
                    var parts = parseRequestLine(stringBuilder.toString());
                    method = parts[0];
                    path = parts[1];
                    version = parts[2];
                    break;
                } else {
                    stringBuilder.append(c);
                }
            }

            if (state != ParseState.HEADER) {
                throw new RequestEntityTooLargeException();
            }


            inputStream.reset();
            // Skip the status line
            inputStream.skipNBytes(readIndex + 1);

            try {
                HeaderBag headers = HttpFieldParser.parseHttpFields(inputStream);
                return Optional.of(
                        new Request(
                                clientAddress,
                                timestamp,
                                method,
                                path,
                                version,
                                headers,
                                getRequestInputStream(headers)
                        ));
            } catch (HttpFieldParseException e) {
                throw new BadRequestException("Invalid header");
            } finally {
                requestCount++;
            }

        } catch (SocketTimeoutException e) {
            logger.debug("Closing connection to {} due to timeout", clientAddress);
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream getRequestInputStream(HeaderBag headers) {
        var transferEncoding = headers.get("Transfer-Encoding");
        var contentLength = headers.get("Content-Length");
        var uncloseableInputStream = new UncloseableInputStream(inputStream);

        InputStream requestInputStream = new LimitedInputStream(uncloseableInputStream, 0);

        if (transferEncoding != null && !transferEncoding.equals("identity")) {
            requestInputStream = new ChunkedInputStream(uncloseableInputStream);
        } else if (contentLength != null) {
            requestInputStream = new LimitedInputStream(uncloseableInputStream, Long.parseLong(
                    contentLength));
        }
        return requestInputStream;
    }

    private void drainCRLF() throws IOException {
        int read;
        do {
            inputStream.mark(1);
            read = inputStream.read();
        } while (read == '\r' || read == '\n');
        inputStream.reset();
    }

    private HeaderBag mergeDefaultAndMandatoryHeaders(Response response) {
        var mergedHeaders = new HashMap<String, String>();
        mergedHeaders.putAll(DEFAULT_HEADERS.all());
        mergedHeaders.putAll(response.headers().all());

        if (!mergedHeaders.containsKey("Date")) {
            var formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
            mergedHeaders.put("Date", ZonedDateTime.now().format(formatter));
        }


        mergedHeaders.put("Connection", "keep-alive");
        mergedHeaders.put("Keep-Alive", "timeout=5, max=100");


        return HeaderBag.of(mergedHeaders);
    }

    public void sendResponse(Response response) throws IOException {
        var status = response.status();
        var headers = mergeDefaultAndMandatoryHeaders(response);
        var body = response.body();

        var contentLength = body.getContentLength();

        if (contentLength >= 0) {
            headers.set("Content-Length", String.valueOf(contentLength));
        } else {
            headers.set("Transfer-Encoding", "chunked");
        }


        outputStream.write(
                ("HTTP/1.1 %d %s\r\n".formatted(status.code(), status.message())).getBytes());

        for (var header : headers.all().entrySet()) {
            outputStream.write(
                    ("%s: %s\r\n".formatted(header.getKey(), header.getValue())).getBytes());
        }

        outputStream.write("\r\n".getBytes());

        if (contentLength >= 0) {
            body.writeTo(outputStream);
        } else {
            var chunkedOutputStream = new ChunkedOutputStream(outputStream);
            body.writeTo(chunkedOutputStream);
            chunkedOutputStream.finish();
        }

        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    private enum ParseState {
        STATUS,
        HEADER,
        CR
    }
}
