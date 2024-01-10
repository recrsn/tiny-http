package com.inputforge.tinyhttp.server.messages.handlers;

public class FixedPathMatcher implements PathMatcher {
    private final String path;

    public FixedPathMatcher(String path) {
        this.path = path;
    }

    @Override
    public String prefix() {
        return path;
    }

    @Override
    public boolean matches(String path) {
        return path.equals(this.path);
    }

    @Override
    public String toString() {
        return path;
    }
}
