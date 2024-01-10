package com.inputforge.tinyhttp.server.messages.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {
    private long limit;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        if (limit <= 0) {
            return -1;
        }
        int result = super.read();
        if (result != -1) {
            limit--;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (limit <= 0) {
            return -1;
        }
        len = (int) Math.min(len, limit);
        int result = super.read(b, off, len);
        if (result != -1) {
            limit -= result;
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        n = Math.min(n, limit);
        long result = super.skip(n);
        limit -= result;
        return result;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(super.available(), limit);
    }
}
