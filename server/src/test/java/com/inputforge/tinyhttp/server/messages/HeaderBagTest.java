package com.inputforge.tinyhttp.server.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeaderBagTest {

    @Test
    void caseInsetivity() {
        HeaderBag bag = HeaderBag.of();
        bag.set("Content-Type", "text/html");
        bag.set("content-type", "text/plain");
        assertEquals("text/plain", bag.get("content-type"));
        assertTrue(bag.has("cOnTEnT-TyPe"));
    }
}