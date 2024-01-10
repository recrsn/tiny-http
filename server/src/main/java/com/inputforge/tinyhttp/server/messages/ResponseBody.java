package com.inputforge.tinyhttp.server.messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public interface ResponseBody {
    static ResponseBody of(byte[] body) {
        return new DefaultResponseBody(body);
    }

    static ResponseBody of(String body) {
        return ResponseBody.of(body.getBytes());
    }

    static ResponseBody of(InputStream inputStream) {
        return outputStream -> {
            try (inputStream) {
                inputStream.transferTo(outputStream);
            }
        };
    }

    static ResponseBody of(InputStream inputStream, long streamSize) {
        return new ResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                try (inputStream) {
                    inputStream.transferTo(outputStream);
                }
            }

            @Override
            public long getContentLength() {
                return streamSize;
            }
        };
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

    static ResponseBody streaming(Consumer<OutputStream> outputStreamConsumer) {
        return outputStreamConsumer::accept;
    }

    default long getContentLength() {
        return -1;
    }

    void writeTo(OutputStream outputStream) throws IOException;
}
