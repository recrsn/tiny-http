package com.inputforge.tinyhttp.server.messages;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Instant;

public record Request(
        InetSocketAddress remoteAddress,
        Instant timestamp,
        String method,
        String path,
        String version,
        HeaderBag headers,
        InputStream body) {
}
