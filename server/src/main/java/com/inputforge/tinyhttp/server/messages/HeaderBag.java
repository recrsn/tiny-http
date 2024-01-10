package com.inputforge.tinyhttp.server.messages;

import java.util.HashMap;
import java.util.Map;

public class HeaderBag {
    private final Map<String, String> headers;

    public HeaderBag(Map<String, String> headers) {
        this.headers = normalize(headers);
    }

    private static Map<String, String> normalize(Map<String, String> headers) {
        var normalizedHeaders = new HashMap<String, String>();
        for (var header : headers.entrySet()) {
            normalizedHeaders.put(header.getKey().toLowerCase(), header.getValue());
        }
        return normalizedHeaders;
    }

    public static HeaderBag of(Map<String, String> headers) {
        return new HeaderBag(headers);
    }

    public static HeaderBag of() {
        return new HeaderBag(new HashMap<>());
    }

    public static HeaderBag empty() {
        return new HeaderBag(Map.of());
    }

    public String get(String name) {
        return headers.get(name.toLowerCase());
    }

    public boolean has(String name) {
        return headers.containsKey(name.toLowerCase());
    }

    public Map<String, String> all() {
        return headers;
    }

    public void set(String name, String value) {
        headers.put(name.toLowerCase(), value);
    }
}
