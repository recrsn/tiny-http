package com.inputforge.tinyhttp.server.messages.io;

import com.inputforge.tinyhttp.server.messages.ChunkedEncodingException;
import com.inputforge.tinyhttp.server.messages.HttpFieldParseException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkedInputStreamTest {
    @Test
    void testReadChunks() throws Exception {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                """
                        4\r
                        Wiki\r
                        5\r
                        pedia\r
                        D\r
                         in\r
                        \r
                        chunks\r
                        0\r
                        \r
                        """.getBytes()));
        assertEquals("Wikipedia in\r\n\r\nchunks", new String(in.readAllBytes()));
    }

    @Test
    void testReadOneByte() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                "4\r\nWiki\r\n0\r\n\r\n".getBytes()));
        assertEquals('W', in.read());
        assertEquals('i', in.read());
        assertEquals('k', in.read());
        assertEquals('i', in.read());
        assertEquals(-1, in.read());
    }


    @Test
    void testReadEmptyStream() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                "0\r\n\r\n".getBytes()));
        assertEquals("", new String(in.readAllBytes()));
    }

    @Test
    void testReadEmptySource() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(new byte[0]));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Unexpected end of stream", exception.getMessage());
    }

    @Test
    void testUnexpectedEndOfStream() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("4\r\nWik\r\n".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Unexpected end of stream", exception.getMessage());
    }

    @Test
    void testInvalidChunk() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("4\r\nWiki\n0\r\n\r\n".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Invalid chunk", exception.getMessage());
    }

    @Test
    void testInvalidChunkSize() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("achunk".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Invalid chunk size", exception.getMessage());
    }

    @Test
    void testInvalidChunkLength() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("4\r\nWiki0\r\n\r\n".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Invalid chunk", exception.getMessage());
    }

    @Test
    void testIncompleteChunkLength() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("4".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Unexpected end of stream", exception.getMessage());
    }

    @Test
    void testInvalidChunkHeader() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("4\rabcd".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Invalid chunk header", exception.getMessage());
    }

    @Test
    void testIncompleteChunkHeader() {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream("4\r".getBytes()));
        var exception = assertThrows(ChunkedEncodingException.class, in::readAllBytes);
        assertEquals("Unexpected end of stream", exception.getMessage());
    }

    @Test
    void testIgnoreChunkExtensions() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                "4;k=v\r\nWiki\r\n0\r\n\r\n".getBytes()));
        assertEquals("Wiki", new String(in.readAllBytes()));
    }

    @Test
    void testReadTrailers() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                "4\r\nWiki\r\n0\r\nHeader: value\r\n\r\n".getBytes()));
        assertEquals("Wiki", new String(in.readAllBytes()));
        assertEquals("value", in.getTrailers().get("Header"));
    }

    @Test
    void testInvalidTrailers() {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                "4\r\nWiki\r\n0\r\nHeadervalue\r\n\r\n".getBytes()));
        var exception = assertThrows(HttpFieldParseException.class, in::readAllBytes);
        assertEquals("Invalid field", exception.getMessage());
    }

    @Test
    void testTryReadTrailersBeforeEndOfStream() {
        ChunkedInputStream in = new ChunkedInputStream(new ByteArrayInputStream(
                "4\r\nWiki\r\n0\r\nHeader: value\r\n\r\n".getBytes()));
        var exception = assertThrows(IllegalStateException.class, in::getTrailers);
        assertEquals("Trailers not available yet", exception.getMessage());
    }
}