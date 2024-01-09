package com.inputforge.tinyhttp.server;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChunkedOutputStream extends FilterOutputStream {
    private boolean finished = false;

    public ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (finished) {
            throw new IOException("Stream finished");
        }

        if (len == 0) {
            return;
        }

        String hex = Integer.toHexString(len);
        out.write(hex.getBytes());
        out.write("\r\n".getBytes());
        out.write(b, off, len);
        out.write("\r\n".getBytes());
    }

    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte) b;
        write(buf, 0, 1);
    }

    public void finish() throws IOException {
        if (finished) {
            return;
        }
        out.write("0\r\n\r\n".getBytes());
        finished = true;
    }

    public void close() throws IOException {
        finish();
        super.close();
    }
}
