package com.inputforge.tinyhttp.server.messages.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ChunkedOutputStreamTest {

    @Test
    void testWriteChunks() throws Exception {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var out = new ChunkedOutputStream(byteArrayOutputStream);
        out.write("Wiki".getBytes());
        out.write("pedia".getBytes());
        out.write(" in\r\n\r\nchunks".getBytes());
        out.close();
        assertEquals("4\r\nWiki\r\n5\r\npedia\r\nD\r\n in\r\n\r\nchunks\r\n0\r\n\r\n", byteArrayOutputStream.toString());
    }

    @Test
    void testWriteEmpty() throws Exception {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var out = new ChunkedOutputStream(byteArrayOutputStream);
        out.close();
        assertEquals("0\r\n\r\n", byteArrayOutputStream.toString());
    }

    @Test
    void testWriteSingleByte() throws Exception {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var out = new ChunkedOutputStream(byteArrayOutputStream);
        out.write('W');
        out.write('i');
        out.write('k');
        out.write('i');
        out.close();
        assertEquals("1\r\nW\r\n1\r\ni\r\n1\r\nk\r\n1\r\ni\r\n0\r\n\r\n", byteArrayOutputStream.toString());
    }

    @Test
    void testWriteAfterFinish() {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var out = new ChunkedOutputStream(byteArrayOutputStream);
        var exception = assertThrows(IOException.class, () -> {
            out.write("Wiki".getBytes());
            out.finish();
            out.flush();

            out.write('W');
            out.write('i');
            out.write('k');
            out.write('i');
            out.close();
        });
        assertEquals("Stream finished", exception.getMessage());
        assertEquals("4\r\nWiki\r\n0\r\n\r\n", byteArrayOutputStream.toString());
    }
}