package com.inputforge.tinyhttp.server.messages.handlers;

import java.util.Iterator;

public class PathTokenizer implements Iterable<String> {

    private final String path;

    public PathTokenizer(String path) {
        this.path = path;
    }

    @Override
    public Iterator<String> iterator() {
        return new PathTokenizerIterator(path);
    }

    private static class PathTokenizerIterator implements Iterator<String> {
        private final String path;
        private int index = 0;

        public PathTokenizerIterator(String path) {
            this.path = path;
        }

        @Override
        public boolean hasNext() {
            return index < path.length();
        }

        @Override
        public String next() {
            int start = index;
            while (index < path.length() && path.charAt(index) != '/') {
                index++;
            }
            String token = path.substring(start, index);
            index++;
            return token;
        }
    }
}
