package com.inputforge.tinyhttp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestParser {
    // Headers must be less than 8KB
    public static final int MAX_HEADER_SIZE = 8 * 1024;
    private final InetSocketAddress clientAddress;
    private final InputStream inputStream;
    private final byte[] buffer = new byte[MAX_HEADER_SIZE];
    Logger logger = LoggerFactory.getLogger(RequestParser.class);
    private int requestCount = 0;

    public RequestParser(InetSocketAddress clientAddress, InputStream inputStream) {
        this.clientAddress = clientAddress;
        this.inputStream = new BufferedInputStream(inputStream);
    }


    public Optional<Request> next() {
        if (requestCount > 1000) {
            return Optional.empty();
        }

        try {
            Instant timestamp = Instant.now();
            StringBuilder stringBuilder = new StringBuilder();
            int readIndex;

//            inputStream.mark(buffer.length);

            int read = inputStream.read(buffer);

            if (read == -1) {
                return Optional.empty();
            }
            logger.debug("Read {} bytes", read);
            logger.debug("{}", new String(buffer, 0, read));

            ParseState state = ParseState.METHOD;

            String method = null;
            String path = null;
            String version = null;
            List<String> headers = new ArrayList<>();

            for (readIndex = 0; readIndex < read; readIndex++) {
                char c = (char) buffer[readIndex];

                switch (state) {
                    case METHOD:
                        if (c == ' ') {
                            state = ParseState.PATH;
                            method = stringBuilder.toString();
                            stringBuilder.setLength(0);
                        } else {
                            if (Character.isLetter(c)) {
                                stringBuilder.append(c);
                            } else {
                                throw new BadRequestException("Invalid method");
                            }
                        }
                        break;
                    case PATH:
                        if (c == ' ') {
                            state = ParseState.VERSION;
                            path = stringBuilder.toString();
                            stringBuilder.setLength(0);
                        } else if (c != '\r' && c != '\n') {
                            stringBuilder.append(c);
                        } else {
                            throw new BadRequestException("Invalid path");
                        }
                        break;
                    case VERSION:
                        if (c == '\r') {
                            state = ParseState.VERSION_CR;
                            version = stringBuilder.toString();

                            String[] parts = version.split("/");
                            if (parts.length != 2) {
                                throw new BadRequestException("Invalid version");
                            }
                            if (!parts[0].equals("HTTP")) {
                                throw new BadRequestException("Invalid version");
                            }
                            stringBuilder.setLength(0);
                        } else if (c == '\n') {
                            // Relaxed parsing, allow missing CR
                            state = ParseState.HEADER;
                        } else {
                            stringBuilder.append(c);
                        }
                        break;
                    case VERSION_CR:
                        if (c == '\n') {
                            state = ParseState.HEADER;
                        } else {
                            throw new BadRequestException("Invalid header");
                        }
                        break;
                    case HEADER:
                        if (c != '\r' && c != '\n') {
                            stringBuilder.append(c);
                            break;
                        }
                        if (c == '\r') {
                            state = ParseState.HEADER_CR;
                            break;
                        }
                    case HEADER_CR:
                        if (c == '\n') {
                            var string = stringBuilder.toString();
                            if (string.isEmpty()) {
                                state = ParseState.BODY;
                                break;
                            }

                            headers.add(string);
                            stringBuilder.setLength(0);
                            state = ParseState.HEADER;
                        } else {
                            throw new BadRequestException("Invalid header");
                        }
                        break;
                }
            }

            if (state != ParseState.BODY) {
                throw new RequestEntityTooLargeException();
            }

            // Connection pipelining is not supported yet
            // inputStream.reset();
            // inputStream.skipNBytes(readIndex);

            requestCount++;
            return Optional.of(
                    new Request(
                            clientAddress.getAddress(),
                            timestamp,
                            method,
                            path,
                            version,
                            parseHeaders(headers),
                            inputStream
                    ));
        } catch (SocketTimeoutException e) {
            logger.debug("Closing connection to {} due to timeout", clientAddress);
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private HeaderBag parseHeaders(List<String> rawHeaders) {
        return HeaderBag.of(rawHeaders.stream()
                .map(rawHeader -> rawHeader.split(":"))
                .collect(Collectors.toMap(header -> header[0].trim(), header -> header[1].trim())));
    }

    private enum ParseState {
        METHOD, PATH, VERSION, VERSION_CR, HEADER, BODY, HEADER_CR
    }
}
