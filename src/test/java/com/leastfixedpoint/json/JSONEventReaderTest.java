package com.leastfixedpoint.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JSONEventReaderTest {
    @Test
    public void testTokenization() throws IOException {
        JSONReader jsonReader = new JSONReader(new StringReader(
                        "{\n" +
                        "  \"a\":[123, 234],\n" +
                        "  \"b\":true\n" +
                        "}"));
        List<Object> lexemes = new ArrayList<>();
        while (true) {
            try {
                lexemes.add(jsonReader.nextLexeme());
            } catch (EOFException _ioe) {
                break;
            }
        }
        Object[] expected = new Object[] {
                JSONReader.Lexeme.OBJECT_START,
                "a",
                JSONReader.Lexeme.COLON,
                JSONReader.Lexeme.ARRAY_START,
                new BigDecimal(123),
                JSONReader.Lexeme.COMMA,
                new BigDecimal(234),
                JSONReader.Lexeme.ARRAY_END,
                JSONReader.Lexeme.COMMA,
                "b",
                JSONReader.Lexeme.COLON,
                true,
                JSONReader.Lexeme.OBJECT_END
        };
        assertArrayEquals(lexemes.toArray(), expected);
    }

    @Test
    public void testEventReader1() throws IOException {
        JSONReader jsonReader = new JSONReader(new StringReader(
                        "{\n" +
                        "  \"a\":[123, {\"z\":[[],{}]}],\n" +
                        "  \"b\":true\n" +
                        "}"));
        JSONEventReader e = new JSONEventReader(jsonReader);
        assertEquals(e.next(), JSONReader.Lexeme.OBJECT_START);
        assertEquals(e.next(), "a");
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_START);
        assertEquals(e.next(), new BigDecimal(123));
        assertEquals(e.next(), JSONReader.Lexeme.OBJECT_START);
        assertEquals(e.next(), "z");
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_START);
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_START);
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_END);
        assertEquals(e.next(), JSONReader.Lexeme.OBJECT_START);
        assertEquals(e.next(), JSONReader.Lexeme.OBJECT_END);
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_END);
        assertEquals(e.next(), JSONReader.Lexeme.OBJECT_END);
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_END);
        assertEquals(e.next(), "b");
        assertEquals(e.next(), true);
        assertEquals(e.next(), JSONReader.Lexeme.OBJECT_END);
        assertNull(e.next());
        assertNull(e.next()); // yes, a second time
    }

    @Test
    public void testObjectBoundary() throws IOException {
        JSONEventReader e = new JSONEventReader(new JSONReader(new StringReader("[1,2]true[]")));
        assertTrue(e.atBoundary());
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_START);
        assertFalse(e.atBoundary());
        assertEquals(e.next(), new BigDecimal(1));
        assertFalse(e.atBoundary());
        assertEquals(e.next(), new BigDecimal(2));
        assertFalse(e.atBoundary());
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_END);
        assertTrue(e.atBoundary());
        assertEquals(e.next(), true);
        assertTrue(e.atBoundary());
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_START);
        assertFalse(e.atBoundary());
        assertEquals(e.next(), JSONReader.Lexeme.ARRAY_END);
        assertTrue(e.atBoundary());
    }
}
