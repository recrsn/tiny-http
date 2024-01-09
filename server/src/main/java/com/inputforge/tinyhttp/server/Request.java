package com.inputforge.tinyhttp.server;

import java.io.InputStream;
import java.net.InetAddress;
import java.time.Instant;

public record Request(
        InetAddress remoteAddress,
        Instant timestamp,
        String method,
        String path,
        String version,
        HeaderBag headers,
        InputStream body) {
}
