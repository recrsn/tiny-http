package com.inputforge.tinyhttp.server.messages.io;

import com.inputforge.tinyhttp.server.messages.ChunkedEncodingException;
import com.inputforge.tinyhttp.server.messages.HeaderBag;
import com.inputforge.tinyhttp.server.messages.HttpFieldParser;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ChunkedInputStream extends FilterInputStream {

    private HeaderBag trailers = null;
    private long currentChunkSize = -1;

    public ChunkedInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (currentChunkSize == -1) {
            readChunkSize();
        }

        if (currentChunkSize == 0) {
            return -1;
        }

        len = (int) Math.min(len, currentChunkSize);
        int result = super.read(b, off, len);
        if (result != -1) {
            currentChunkSize -= result;
        }
        return result;
    }

    private void readChunkSize() throws IOException {
        currentChunkSize = 0;
        byte[] buffer = new byte[1024];
        int readIndex;
        int read;
        boolean chunkSizeRead = false;

        do {
            read = super.read(buffer);

            if (read == -1) {
                throw new ChunkedEncodingException("Chunked input stream ended unexpectedly");
            }

            readIndex = 0;

            while (readIndex < read && buffer[readIndex] != '\r') {
                if (!chunkSizeRead) {
                    currentChunkSize *= 16;
                    if (buffer[readIndex] >= '0' && buffer[readIndex] <= '9') {
                        currentChunkSize += buffer[readIndex] - '0';
                    } else if (buffer[readIndex] >= 'a' && buffer[readIndex] <= 'f') {
                        currentChunkSize += buffer[readIndex] - 'a' + 10;
                    } else if (buffer[readIndex] >= 'A' && buffer[readIndex] <= 'F') {
                        currentChunkSize += buffer[readIndex] - 'A' + 10;
                    } else if (buffer[readIndex] == ';') {
                        chunkSizeRead = true;
                    } else {
                        throw new ChunkedEncodingException("Invalid chunk size");
                    }
                }
                // skip chunk extensions
                readIndex++;
            }
        } while (readIndex == read);

        read = super.read();

        if (read == -1) {
            throw new ChunkedEncodingException("Chunked input stream ended unexpectedly");
        }
        if (read != '\n') {
            throw new ChunkedEncodingException("Invalid chunk size");
        }

        if (currentChunkSize == 0) {
            // read trailer
            readTrailers();
        }
    }

    private void readTrailers() throws IOException {
        trailers = HttpFieldParser.parseHttpFields(super.in);
    }

    public HeaderBag getTrailers() {
        if (trailers == null) {
            throw new IllegalStateException("Trailers not available yet");
        }

        return trailers;
    }
}
