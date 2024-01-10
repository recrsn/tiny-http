package com.inputforge.tinyhttp.server.messages.io;

import java.io.FilterInputStream;
import java.io.InputStream;

public class UncloseableInputStream extends FilterInputStream {
    public UncloseableInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
