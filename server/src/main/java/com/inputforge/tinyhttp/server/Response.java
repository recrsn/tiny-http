package com.inputforge.tinyhttp.server;

import java.util.Map;

public record Response(
        HttpStatus status,
        HeaderBag headers,
        ResponseBody body
) {
}
