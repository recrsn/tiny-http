package com.inputforge.tinyhttp.server.messages.handlers;

public class PrefixPathMatcher implements PathMatcher {
    private final String prefix;

    public PrefixPathMatcher(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public boolean matches(String path) {
        return path.startsWith(prefix);
    }

    @Override
    public String toString() {
        return prefix + "*";
    }
}
