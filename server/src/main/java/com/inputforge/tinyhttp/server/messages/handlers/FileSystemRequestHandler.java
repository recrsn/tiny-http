package com.inputforge.tinyhttp.server.messages.handlers;

import com.inputforge.tinyhttp.server.messages.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileSystemRequestHandler implements RequestHandler {

    private final Path root;
    private final String base;

    public FileSystemRequestHandler(String base, Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("root must be a directory");
        }
        this.base = base;
        this.root = root.normalize();
    }

    @Override
    public Response handle(Request request) {
        if (!request.path().startsWith(base)) {
            throw new NotFoundException();
        }

        Path path = root.resolve("./" + request.path().substring(base.length())).toAbsolutePath();

        if (!path.startsWith(root)) {
            // Prevent path traversal
            throw new NotFoundException();
        }

        if (Files.isDirectory(path)) {
            path = path.resolve("index.html");
        }

        if (!Files.exists(path)) {
            throw new NotFoundException();
        }
        try {
            return new Response(
                    HttpStatus.OK,
                    HeaderBag.of(Map.of(
                            "Content-Type", Files.probeContentType(path),
                            "Content-Length", String.valueOf(Files.size(path))
                    )),
                    ResponseBody.of(Files.newInputStream(path), Files.size(path)
                    )
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
