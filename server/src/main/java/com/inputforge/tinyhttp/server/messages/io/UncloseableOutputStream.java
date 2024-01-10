package com.inputforge.tinyhttp.server.messages.io;

import java.io.FilterOutputStream;
import java.io.OutputStream;

public class UncloseableOutputStream extends FilterOutputStream {
    public UncloseableOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
