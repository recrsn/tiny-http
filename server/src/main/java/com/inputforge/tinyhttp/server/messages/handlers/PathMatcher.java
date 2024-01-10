package com.inputforge.tinyhttp.server.messages.handlers;

public interface PathMatcher {
    static PathMatcher path(String path) {
        return path.contains("*") ? pattern(path) : fixed(path);
    }

    static PathMatcher fixed(String path) {
        return new FixedPathMatcher(path);
    }

    static PathMatcher pattern(String pattern) {
        return new PatternPathMatcher(pattern);
    }


    static PathMatcher prefix(String prefix) {
        return new PrefixPathMatcher(prefix);
    }

    String prefix();

    boolean matches(String path);
}
