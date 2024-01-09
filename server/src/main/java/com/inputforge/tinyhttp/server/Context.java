package com.inputforge.tinyhttp.server;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<Key<?>, Object> attributes;

    public Context() {
        this(Map.of());
    }

    public Context(Map<Key<?>, Object> attributes) {
        this.attributes = attributes;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        return (T) attributes.get(key);
    }

    public <T> Context with(Key<T> key, T value) {
        var newAttributes = new HashMap<>(attributes);
        newAttributes.put(key, value);
        return new Context(newAttributes);
    }

    public record Key<T>(String name) {
        public static <T> Key<T> of(String name) {
            return new Key<>(name);
        }
    }

}
