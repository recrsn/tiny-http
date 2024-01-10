package com.inputforge.tinyhttp.server.messages.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LimitedInputStreamTest {
    @Test
    void testReadSingleByte() throws IOException {
        var byteArray = "Hello World!".getBytes();
        var in = new LimitedInputStream(new ByteArrayInputStream(byteArray), 5);
        assertEquals('H', in.read());
        assertEquals('e', in.read());
        assertEquals('l', in.read());
        assertEquals('l', in.read());
        assertEquals('o', in.read());
        assertEquals(-1, in.read());
    }

    @Test
    void testReadAllBytes() throws IOException {
        var byteArray = "Hello World!".getBytes();
        var in = new LimitedInputStream(new ByteArrayInputStream(byteArray), 5);
        assertEquals("Hello", new String(in.readAllBytes()));
    }

    @Test
    void testAvailable() throws IOException {
        var byteArray = "Hello World!".getBytes();
        var in = new LimitedInputStream(new ByteArrayInputStream(byteArray), 5);
        assertEquals(5, in.available());
        assertEquals('H', in.read());
        assertEquals(4, in.available());
        assertEquals('e', in.read());
        assertEquals(3, in.available());
        assertEquals('l', in.read());
        assertEquals(2, in.available());
        assertEquals('l', in.read());
        assertEquals(1, in.available());
        assertEquals('o', in.read());
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
        assertEquals(0, in.available());
    }

    @Test
    void testSkip() throws IOException {
        var byteArray = "Hello World!".getBytes();
        var in = new LimitedInputStream(new ByteArrayInputStream(byteArray), 5);
        assertEquals(3, in.skip(3));
        assertEquals('l', in.read());
        assertEquals('o', in.read());
        assertEquals(-1, in.read());
    }

    @Test
    void testSkipMoreThanLimit() throws IOException {
        var byteArray = "Hello World!".getBytes();
        var in = new LimitedInputStream(new ByteArrayInputStream(byteArray), 5);
        assertEquals(5, in.skip(10));
        assertEquals(-1, in.read());
    }

}