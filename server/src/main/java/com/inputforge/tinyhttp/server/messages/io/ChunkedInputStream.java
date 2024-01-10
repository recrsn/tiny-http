package com.inputforge.tinyhttp.server.messages.io;

import com.inputforge.tinyhttp.server.messages.ChunkedEncodingException;
import com.inputforge.tinyhttp.server.messages.HeaderBag;
import com.inputforge.tinyhttp.server.messages.HttpFieldParser;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ChunkedInputStream extends FilterInputStream {

    private HeaderBag trailers = null;
    private long currentChunkSize = 0;
    private boolean finished = false;

    public ChunkedInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int read = read(buffer);
        if (read == -1) {
            return -1;
        }
        return buffer[0] & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (finished) {
            return -1;
        }

        if (currentChunkSize == 0) {
            readChunkSize();
        }

        len = (int) Math.min(len, currentChunkSize);
        int result = super.read(b, off, len);
        if (result != -1) {
            currentChunkSize -= result;
        }

        if (currentChunkSize == 0 && !finished) {
            // read \r\n at the end of the chunk
            int b1 = in.read();
            int b2 = in.read();

            if (b1 == -1 || b2 == -1) {
                throw new ChunkedEncodingException("Unexpected end of stream");
            }
            if (b1 != '\r' || b2 != '\n') {
                throw new ChunkedEncodingException("Invalid chunk");
            }
        }

        return result;
    }

    private void readChunkSize() throws IOException {
        currentChunkSize = 0;
        int readIndex;
        int read;
        boolean chunkSizeRead = false;
        byte[] buffer = new byte[1024];

        do {
            in.mark(buffer.length + 1);
            read = in.read(buffer);

            if (read == -1) {
                throw new ChunkedEncodingException("Unexpected end of stream");
            }

            readIndex = 0;

            while (readIndex < read && buffer[readIndex] != '\r') {
                if (!chunkSizeRead) {
                    if (buffer[readIndex] >= '0' && buffer[readIndex] <= '9') {
                        currentChunkSize = currentChunkSize * 16 + buffer[readIndex] - '0';
                    } else if (buffer[readIndex] >= 'a' && buffer[readIndex] <= 'f') {
                        currentChunkSize = currentChunkSize * 16 + buffer[readIndex] - 'a' + 10;
                    } else if (buffer[readIndex] >= 'A' && buffer[readIndex] <= 'F') {
                        currentChunkSize = currentChunkSize * 16 + buffer[readIndex] - 'A' + 10;
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

        in.reset();
        in.skipNBytes(readIndex + 1);

        // Check for \n at the end of the chunk size
        int b = in.read();

        if (b == -1) {
            throw new ChunkedEncodingException("Unexpected end of stream");
        }
        if (b != '\n') {
            throw new ChunkedEncodingException("Invalid chunk header");
        }


        if (currentChunkSize == 0) {
            // read trailer
            finished = true;
            readTrailers();
        }
    }

    private void readTrailers() throws IOException {
        in.mark(1);
        int b = in.read();
        if (b == -1) {
            return;
        }
        in.reset();
        trailers = HttpFieldParser.parseHttpFields(super.in);
    }

    public HeaderBag getTrailers() {
        if (trailers == null) {
            throw new IllegalStateException("Trailers not available yet");
        }

        return trailers;
    }
}
