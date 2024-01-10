package com.inputforge.tinyhttp.server.messages.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrefixTreeRouter extends Router {
    private final Node root = new Node(null);

    public PrefixTreeRouter(List<Route> routes) {
        routes.forEach(this::add);
    }

    private void add(Route handler) {
        Node node = root;

        var prefix = handler.matcher().prefix();
        var tokenizer = new PathTokenizer(prefix);

        for (String part : tokenizer) {
            node = node.children.computeIfAbsent(part, Node::new);
        }

        node.routes.add(handler);
    }

    public List<Route> findCandidateRoutes(String path) {
        List<Route> handlers = new ArrayList<>();

        Node node = root;
        for (String part : new PathTokenizer(path)) {
            node = node.children.get(part);
            if (node == null) {
                break;
            }
            node.routes.stream()
                    .filter(h -> h.matcher().matches(path))
                    .forEach(handlers::add);
        }
        return handlers;
    }

    private record Node(
            String prefix,
            List<Route> routes,
            Map<String, Node> children) {

        public Node(String prefix) {
            this(prefix, new ArrayList<>(), new HashMap<>());
        }
    }

}
