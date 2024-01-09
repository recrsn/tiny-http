package com.inputforge.tinyhttp.server;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ResponseWriter {

    public static final Context.Key<Boolean> FINAL_RESPONSE = Context.Key.of("final-response");
    private static final HeaderBag DEFAULT_HEADERS = HeaderBag.of(Map.of(
            "Server", "TinyHttp"
    ));

    private HeaderBag mergeDefaultAndMandatoryHeaders(Response response) {
        var mergedHeaders = new HashMap<String, String>();
        mergedHeaders.putAll(DEFAULT_HEADERS.all());
        mergedHeaders.putAll(response.headers().all());

        if (!mergedHeaders.containsKey("Date")) {
            var formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
            mergedHeaders.put("Date", ZonedDateTime.now().format(formatter));
        }


        mergedHeaders.put("Connection", "keep-alive");
        mergedHeaders.put("Keep-Alive", "timeout=5, max=100");


        return HeaderBag.of(mergedHeaders);
    }

    public void write(Response response, OutputStream outputStream) throws IOException {
        var status = response.status();
        var headers = mergeDefaultAndMandatoryHeaders(response);
        var body = response.body();

        var contentLength = body.getContentLength();

        if (contentLength >= 0) {
            headers.set("Content-Length", String.valueOf(contentLength));
        } else {
            headers.set("Transfer-Encoding", "chunked");
        }


        outputStream.write(
                ("HTTP/1.1 %d %s\r\n".formatted(status.code(), status.message())).getBytes());

        for (var header : headers.all().entrySet()) {
            outputStream.write(
                    ("%s: %s\r\n".formatted(header.getKey(), header.getValue())).getBytes());
        }

        outputStream.write("\r\n".getBytes());

        if (contentLength >= 0) {
            body.writeTo(outputStream);
        } else {
            var chunkedOutputStream = new ChunkedOutputStream(outputStream);
            body.writeTo(chunkedOutputStream);
            chunkedOutputStream.finish();
        }

        outputStream.flush();
    }
}
