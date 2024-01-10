package com.inputforge.tinyhttp.server.messages.handlers;

import java.util.regex.Pattern;

public class PatternPathMatcher implements PathMatcher {
    private final String pattern;
    private final Pattern regex;

    public PatternPathMatcher(String pattern) {
        this.pattern = pattern;
        this.regex = Pattern.compile(pattern.replaceAll("\\*+", "(.+)"));
    }

    @Override
    public boolean matches(String path) {
        return regex.matcher(path).matches();
    }

    @Override
    public String toString() {
        return pattern;
    }
}
