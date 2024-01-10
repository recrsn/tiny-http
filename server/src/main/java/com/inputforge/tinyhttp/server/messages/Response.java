package com.inputforge.tinyhttp.server.messages;

public record Response(
        HttpStatus status,
        HeaderBag headers,
        ResponseBody body
) {
    public Response(HttpStatus status) {
        this(status, HeaderBag.empty(), ResponseBody.empty());
    }

    public static Response notFound() {
        return new Response(HttpStatus.NOT_FOUND);
    }

    public static Response noContent() {
        return new Response(HttpStatus.NO_CONTENT);
    }

    public static Response internalServerError() {
        return new Response(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
