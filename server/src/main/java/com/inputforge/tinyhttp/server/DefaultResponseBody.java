package com.inputforge.tinyhttp.server;

import java.io.IOException;
import java.io.OutputStream;

public class DefaultResponseBody implements ResponseBody {
    private final byte[] body;

    public DefaultResponseBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(body);
    }

    @Override
    public long getContentLength() {
        return body.length;
    }
}
