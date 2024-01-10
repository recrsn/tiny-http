package com.inputforge.tinyhttp.server.messages;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class HttpFieldParser {
    public static final int MAX_HEADER_SIZE = 8 * 1024;

    public static HeaderBag parseHttpFields(InputStream inputStream) throws IOException {
        HeaderBag headers = new HeaderBag(Map.of());
        byte[] buffer = new byte[MAX_HEADER_SIZE];
        int index = 0;
        boolean readAllHeaders = false;

        StringBuilder stringBuilder = new StringBuilder();

        inputStream.mark(buffer.length + 1);

        // Read first few bytes and expect them to be a header
        while (index < buffer.length) {
            int read = inputStream.read(buffer, index, buffer.length - index);

            if (read == -1) {
                throw new HttpFieldParseException("Stream ended unexpectedly");
            }

            int readIndex = 0;
            boolean readCR = false;

            while (readIndex < read) {
                // Read line until \r or \n
                if (readCR && buffer[readIndex] != '\n') {
                    throw new HttpFieldParseException("Invalid field");
                } else if (buffer[readIndex] == '\n') {
                    var line = stringBuilder.toString();

                    if (line.isBlank()) {
                        readAllHeaders = true;
                        break;
                    }

                    String[] parts = line.split(":", 2);
                    if (parts.length != 2) {
                        throw new HttpFieldParseException("Invalid field");
                    }
                    headers.set(parts[0].trim(), parts[1].trim());
                    stringBuilder.setLength(0);
                    readCR = false;
                } else if (buffer[readIndex] == '\r') {
                    readCR = true;
                } else {
                    stringBuilder.append((char) buffer[readIndex]);
                }
                readIndex++;
            }
            index += readIndex;

            if (readAllHeaders) {
                break;
            }
        }

        if (!readAllHeaders) {
            throw new ContentTooLargeException();
        }

        inputStream.reset();
        inputStream.skipNBytes(index + 1);

        return headers;
    }
}
