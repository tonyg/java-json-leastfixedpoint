package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;

import static org.testng.Assert.*;

public class JSONReaderTest {
    @Test
    public void testNumbers() throws IOException {
        assert JSONReader.readFrom("0").equals(0.0);
        assert JSONReader.readFrom("0.0000e0").equals(0.0);
        assert JSONReader.readFrom("123").equals(123.0);
        assert JSONReader.readFrom("123.0").equals(123.0);
        assert JSONReader.readFrom("123.125").equals(123.125);
        assert JSONReader.readFrom("-123").equals(-123.0);
        assert JSONReader.readFrom("-123.125").equals(-123.125);
        assert JSONReader.readFrom("-123e2").equals(-12300.0);
        assert JSONReader.readFrom("-123.125e2").equals(-12312.5);
        assert JSONReader.readFrom("-123.125e-2").equals(-1.23125);
        assert JSONReader.readFrom("-1.23E09").equals(-1.23E09);
        assert JSONReader.readFrom("-1.23e9").equals(-1.23E09);
        assert JSONReader.readFrom("-1.23E+09").equals(-1.23E09);
        assert JSONReader.readFrom("1.23E+09").equals(1.23E09);
        assert JSONReader.readFrom("1.23E09").equals(1.23E09);
        assert JSONReader.readFrom("1.23E9").equals(1.23E09);
        assert JSONReader.readFrom("1.23e9").equals(1.23E09);
        assert JSONReader.readFrom("-1.23E-13").equals(-1.23E-13);
        assert JSONReader.readFrom("1.23E-13").equals(1.23E-13);
    }

    @Test
    public void testStrings() throws IOException {
        assert JSONReader.readFrom("\"123\"").equals("123");
        assert JSONReader.readFrom("\"\"").equals("");
        assert JSONReader.readFrom("\"\\\\\"").equals("\\");
        assert JSONReader.readFrom("\"\\\"\"").equals("\"");
        assert JSONReader.readFrom("\"x\\\"x\"").equals("x\"x");
        assert JSONReader.readFrom("\"\n\"").equals("\n");
        assert JSONReader.readFrom("\"\\n\"").equals("\n");
        assert JSONReader.readFrom("\"\\uD834\\udd1e\"").equals("\uD834\uDD1E");
        assert JSONReader.readFrom("\"\uD834\udd1e\"").equals("\uD834\uDD1E");
    }

    @Test
    public void testSimple() throws IOException {
        assert JSONReader.readFrom("true").equals(true);
        assert JSONReader.readFrom("false").equals(false);
        assert JSONReader.readFrom("null") == null;
    }

    @Test
    public void testArray() throws IOException {
        ArrayList a = (ArrayList) JSONReader.readFrom("[]");
        assert a.size() == 0;
        a = (ArrayList) JSONReader.readFrom("[1,null,\"C\"]");
        assert a.size() == 3;
        assert a.get(0).equals(1.0);
        assert a.get(1) == null;
        assert a.get(2).equals("C");
        a = (ArrayList) JSONReader.readFrom("[1, null,\n\"C\"]");
        assert a.size() == 3;
        assert a.get(0).equals(1.0);
        assert a.get(1) == null;
        assert a.get(2).equals("C");
        a = (ArrayList) JSONReader.readFrom("[ [ [] ] ]");
        assert a.size() == 1;
        a = (ArrayList) a.get(0);
        assert a.size() == 1;
        a = (ArrayList) a.get(0);
        assert a.size() == 0;
    }

    @Test
    public void testMap() throws IOException {
        Map m = (Map) JSONReader.readFrom("{}");
        assert m.size() == 0;
        m = (Map) JSONReader.readFrom("{\"a\": 123}");
        assert m.size() == 1;
        assert m.get("a").equals(123.0);
        m = (Map) JSONReader.readFrom("{\"a\": 123, \"b\": [null]}");
        assert m.size() == 2;
        assert m.get("a").equals(123.0);
        assert m.get("b") instanceof ArrayList;
        assert ((ArrayList) m.get("b")).get(0) == null;
        m = (Map) JSONReader.readFrom("{\"a\":123,\"b\":234}");
        assert m.size() == 2;
        assert m.get("a").equals(123.0);
        assert m.get("b").equals(234.0);
    }

    @Test
    public void testEarlyEOF() throws IOException {
        for (String wholeString : new String[] {
                "{\"a\": 123}", "{\"a\": 123, \"\": null, \"b\\\"b\": []}",
                "[1,[2,3],4,5]", "[{},{},{}]",
                "\"ab\\\"cd\"",
                "true", "false", "null"
        }) {
            JSONReader.readFrom(wholeString);
            JSONReader.readFrom(wholeString + "  ");
            for (int limit = 0; limit < wholeString.length(); limit++) {
                String partialString = wholeString.substring(0, limit);
                try {
                    JSONReader.readFrom(partialString);
                } catch (EOFException ee) {
                    continue;
                }
                assert false : "Expected EOFException from EOF case: >>>" + partialString + "<<<";
            }
        }
    }

    @Test(expectedExceptions = {EOFException.class})
    public void testSpecialEOF1() throws IOException {
        JSONReader.readFrom("{\"a\": 123, \"b: 234}");
    }

    @Test
    public void testSyntaxErrors() throws IOException {
        for (String str : new String[] {
                "{a\": 123, \"b\": 234}",
                "{\"a: 123, \"b\": 234}",
                "{\"a\" 123, \"b\": 234}",
                "{\"a\": 123 \"b\": 234}",
                "{\"a\": 123, b\": 234}",
                "{\"a\": 123, \"b\" 234}",
                "{,\"a\": 123, \"b\": 234}",
                "{\"a\": 123, , \"b\": 234}",
                "{\"a\": 123,, \"b\": 234}",
                "{\"a\": 123 ,,\"b\": 234}",
                "{,}",
                "[,2,3]",
                "[1 2,3]",
                "[1,,3]",
                "[1,2 3]",
                "[1,2,3,]",
                "trondheim",
                "flase",
                "nil",
                "\"\\?\""
        }) {
            try {
                JSONReader.readFrom(str);
            } catch (JSONSyntaxError jse) {
                continue;
            }
            assert false : "Expected JSONSyntaxError from malformed case: >>>" + str + "<<<";
        }
    }

    @Test
    public void testComments() throws IOException {
        JSONReader r = new JSONReader(new StringReader("// starting\n123 // ending\n234"));
        assert r.read().equals(123.0);
        assert r.read().equals(234.0);
        try { r.read(); } catch (EOFException ee) { return; }
        assert false : "Expected EOF exception";
    }

    @Test(expectedExceptions = {JSONSyntaxError.class})
    public void testInvalidComment1() throws IOException {
        JSONReader.readFrom("/* invalid */\n 123");
    }

    @Test(expectedExceptions = {JSONSyntaxError.class})
    public void testInvalidComment2() throws IOException {
        JSONReader.readFrom("/- invalid\n 123");
    }
}