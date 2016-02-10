package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;

import static org.testng.Assert.*;

public class JSONReaderTest {
    public void checkRead(String source, Object expected) throws IOException {
	Object actual = JSONReader.readFrom(source);
	assert actual.equals(expected) : "Actual >>>" + actual + "<<< =/= >>>" + expected + "<<<";
    }

    @Test
    public void testNumbers() throws IOException {
        checkRead("0", 0.0);
        checkRead("0.0000e0", 0.0);
        checkRead("123", 123.0);
        checkRead("123.0", 123.0);
        checkRead("123.125", 123.125);
        checkRead("-123", -123.0);
        checkRead("-123.125", -123.125);
        checkRead("-123e2", -12300.0);
        checkRead("-123.125e2", -12312.5);
        checkRead("-123.125e-2", -1.23125);
        checkRead("-1.23E09", -1.23E09);
        checkRead("-1.23e9", -1.23E09);
        checkRead("-1.23E+09", -1.23E09);
        checkRead("1.23E+09", 1.23E09);
        checkRead("1.23E09", 1.23E09);
        checkRead("1.23E9", 1.23E09);
        checkRead("1.23e9", 1.23E09);
        checkRead("-1.23E-13", -1.23E-13);
        checkRead("1.23E-13", 1.23E-13);
    }

    @Test
    public void testStrings() throws IOException {
        checkRead("\"123\"", "123");
        checkRead("\"\"", "");
        checkRead("\"\\\\\"", "\\");
        checkRead("\"\\\"\"", "\"");
        checkRead("\"x\\\"x\"", "x\"x");
        checkRead("\"\n\"", "\n");
        checkRead("\"\\n\"", "\n");
        checkRead("\"\\uD834\\udd1e\"", "\uD834\uDD1E");
        checkRead("\"\uD834\udd1e\"", "\uD834\uDD1E");
        checkRead("\"\\u0080\"", "\u0080");
    }

    @Test
    public void testSimple() throws IOException {
        checkRead("true", true);
        checkRead("false", false);
        assert JSONReader.readFrom("null") == JSONNull.INSTANCE;
    }

    @Test
    public void testArray() throws IOException {
        ArrayList a = (ArrayList) JSONReader.readFrom("[]");
        assert a.size() == 0;
        a = (ArrayList) JSONReader.readFrom("[1,null,\"C\"]");
        assert a.size() == 3;
        assert a.get(0).equals(1.0);
        assert a.get(1) == JSONNull.INSTANCE;
        assert a.get(2).equals("C");
        a = (ArrayList) JSONReader.readFrom("[1, null,\n\"C\"]");
        assert a.size() == 3;
        assert a.get(0).equals(1.0);
        assert a.get(1) == JSONNull.INSTANCE;
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
        assert ((ArrayList) m.get("b")).get(0) == JSONNull.INSTANCE;
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
                "\"\\?\"",
                "/* invalid */123",
                "/* invalid */\n123",
                "/- invalid\n123",
                "}",
                "]"
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
}
