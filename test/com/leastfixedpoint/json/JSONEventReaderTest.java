package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
        assert Arrays.equals(lexemes.toArray(), expected);
    }

    @Test
    public void testEventReader1() throws IOException {
        JSONReader jsonReader = new JSONReader(new StringReader(
                        "{\n" +
                        "  \"a\":[123, {\"z\":[[],{}]}],\n" +
                        "  \"b\":true\n" +
                        "}"));
        JSONEventReader e = new JSONEventReader(jsonReader);
        assert e.next().equals(JSONReader.Lexeme.OBJECT_START);
        assert e.next().equals("a");
        assert e.next().equals(JSONReader.Lexeme.ARRAY_START);
        assert e.next().equals(new BigDecimal(123));
        assert e.next().equals(JSONReader.Lexeme.OBJECT_START);
        assert e.next().equals("z");
        assert e.next().equals(JSONReader.Lexeme.ARRAY_START);
        assert e.next().equals(JSONReader.Lexeme.ARRAY_START);
        assert e.next().equals(JSONReader.Lexeme.ARRAY_END);
        assert e.next().equals(JSONReader.Lexeme.OBJECT_START);
        assert e.next().equals(JSONReader.Lexeme.OBJECT_END);
        assert e.next().equals(JSONReader.Lexeme.ARRAY_END);
        assert e.next().equals(JSONReader.Lexeme.OBJECT_END);
        assert e.next().equals(JSONReader.Lexeme.ARRAY_END);
        assert e.next().equals("b");
        assert e.next().equals(true);
        assert e.next().equals(JSONReader.Lexeme.OBJECT_END);
        assert e.next() == null;
        assert e.next() == null; // yes, a second time
    }

    @Test
    public void testObjectBoundary() throws IOException {
        JSONEventReader e = new JSONEventReader(new JSONReader(new StringReader("[1,2]true[]")));
        assert e.atBoundary();
        assert e.next().equals(JSONReader.Lexeme.ARRAY_START);
        assert !e.atBoundary();
        assert e.next().equals(new BigDecimal(1));
        assert !e.atBoundary();
        assert e.next().equals(new BigDecimal(2));
        assert !e.atBoundary();
        assert e.next().equals(JSONReader.Lexeme.ARRAY_END);
        assert e.atBoundary();
        assert e.next().equals(true);
        assert e.atBoundary();
        assert e.next().equals(JSONReader.Lexeme.ARRAY_START);
        assert !e.atBoundary();
        assert e.next().equals(JSONReader.Lexeme.ARRAY_END);
        assert e.atBoundary();
    }
}
