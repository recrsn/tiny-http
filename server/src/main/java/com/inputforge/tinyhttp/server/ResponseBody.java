package com.inputforge.tinyhttp.server;

import java.io.IOException;
import java.io.OutputStream;

public interface ResponseBody {
    static ResponseBody of(byte[] body) {
        return new DefaultResponseBody(body);
    }

    static ResponseBody of(String body) {
        return ResponseBody.of(body.getBytes());
    }

    static ResponseBody empty() {
        return new ResponseBody() {
            @Override
            public long getContentLength() {
                return 0;
            }

            @Override
            public void writeTo(OutputStream outputStream) {
            }
        };
    }

    default long getContentLength() {
        return -1;
    }

    void writeTo(OutputStream outputStream) throws IOException;
}
