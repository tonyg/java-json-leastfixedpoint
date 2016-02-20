package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONReaderTest {
    public void checkRead(String source, Object expected) throws IOException {
        Object actual = JSONReader.readFrom(source);
        assert actual.equals(expected) : "Actual >>>" + actual + "<<< =/= >>>" + expected + "<<<";
    }

    // Convenience specifically for simple number checks
    public void checkNumber(String source, double expected) throws IOException {
        JSONValue v = JSONValue.wrap(JSONReader.readFrom(source));
        assert v.doubleValue() == expected : "Actual >>>" + v + "<<< =/= >>>" + expected + "<<<";
    }

    @Test
    public void testNumbers() throws IOException {
        checkNumber("0", 0.0);
        checkNumber("0.0000e0", 0.0);
        checkNumber("123", 123.0);
        checkNumber("123.0", 123.0);
        checkNumber("123.125", 123.125);
        checkNumber("-123", -123.0);
        checkNumber("-123.125", -123.125);
        checkNumber("-123e2", -12300.0);
        checkNumber("-123.125e2", -12312.5);
        checkNumber("-123.125e-2", -1.23125);
        checkNumber("-1.23E09", -1.23E09);
        checkNumber("-1.23e9", -1.23E09);
        checkNumber("-1.23E+09", -1.23E09);
        checkNumber("1.23E+09", 1.23E09);
        checkNumber("1.23E09", 1.23E09);
        checkNumber("1.23E9", 1.23E09);
        checkNumber("1.23e9", 1.23E09);
        checkNumber("-1.23E-13", -1.23E-13);
        checkNumber("1.23E-13", 1.23E-13);
        assert JSONReader.readFrom("1234567890123456789012345678901234567890")
            .equals(new BigDecimal("1234567890123456789012345678901234567890"));
        assert JSONReader.readFrom("1E+40").equals(new BigDecimal("1e40"));
        assert JSONReader.readFrom("1e40").equals(new BigDecimal("1e40"));
    }

    @Test
    public void testBOM() throws IOException {
        checkRead("\uFEFF\"123\"", "123");
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
        assert a.get(0).equals(BigDecimal.ONE);
        assert a.get(1) == JSONNull.INSTANCE;
        assert a.get(2).equals("C");
        a = (ArrayList) JSONReader.readFrom("[1, null,\n\"C\"]");
        assert a.size() == 3;
        assert a.get(0).equals(BigDecimal.ONE);
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
    public void testArrayValue() throws IOException {
        JSONValue a = JSONValue.wrap(JSONReader.readFrom("[]"));
        assert a.size() == 0;
        a = JSONValue.wrap(JSONReader.readFrom("[1,null,\"C\"]"));
        assert a.size() == 3;
        assert a.get(0).doubleValue() == 1.0;
        a.get(1).checkNull();
        assert a.get(2).stringValue().equals("C");
        a = JSONValue.wrap(JSONReader.readFrom("[ [ [] ] ]"));
        assert a.size() == 1;
        assert a.get(0).size() == 1;
        assert a.get(0).get(0).size() == 0;
    }

    @Test
    public void testMap() throws IOException {
        Map m = (Map) JSONReader.readFrom("{}");
        assert m.size() == 0;
        m = (Map) JSONReader.readFrom("{\"a\": 123}");
        assert m.size() == 1;
        assert m.get("a").equals(new BigDecimal(123.0));
        m = (Map) JSONReader.readFrom("{\"a\": 123, \"b\": [null]}");
        assert m.size() == 2;
        assert m.get("a").equals(new BigDecimal(123.0));
        assert m.get("b") instanceof ArrayList;
        assert ((ArrayList) m.get("b")).get(0) == JSONNull.INSTANCE;
        m = (Map) JSONReader.readFrom("{\"a\":123,\"b\":234}");
        assert m.size() == 2;
        assert m.get("a").equals(new BigDecimal(123.0));
        assert m.get("b").equals(new BigDecimal(234.0));
    }

    @Test
    public void testMapValue() throws IOException {
        JSONValue m = JSONValue.wrap(JSONReader.readFrom("{}"));
        assert m.size() == 0;
        m = JSONValue.wrap(JSONReader.readFrom("{\"a\": 123}"));
        assert m.size() == 1;
        assert m.get("a").longValue() == 123;
        assert m.get("a").doubleValue() == 123.0;
        m = JSONValue.wrap(JSONReader.readFrom("{\"a\": 123, \"b\": [null]}"));
        assert m.size() == 2;
        assert m.get("a").doubleValue() == 123.0;
        m.get("b").get(0).checkNull();
        m = JSONValue.wrap(JSONReader.readFrom("{\"a\":123,\"b\":234}"));
        assert m.size() == 2;
        assert m.get("a").doubleValue() == 123.0;
        assert m.get("b").doubleValue() == 234.0;
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
        assert r.read().equals(new BigDecimal(123.0));
        assert r.read().equals(new BigDecimal(234.0));
        try { r.read(); } catch (EOFException ee) { return; }
        assert false : "Expected EOF exception";
    }

    @Test(expectedExceptions = {JSONSyntaxError.class})
    public void testTrailingData1() throws IOException {
        checkRead("true love waits", "you had me at 'true'");
    }

    @Test
    public void testTrailingData2() throws IOException {
        assert JSONReader.readFrom("true love waits", false).equals(true);
    }

    @Test
    public void demoInternalBuffer1() throws IOException {
        Reader r = new LineNumberReader(new StringReader("123xy"));
        JSONReader jsonReader = new JSONReader(r);
        assert jsonReader.read().equals(new BigDecimal(123.0));
        assert r.read() == 'y';

        r = new LineNumberReader(new StringReader("[1,2]xy"));
        jsonReader = new JSONReader(r);
        assert jsonReader.read() instanceof List<?>;
        assert r.read() == 'x';

        r = new LineNumberReader(new StringReader("truexy"));
        jsonReader = new JSONReader(r);
        assert jsonReader.read().equals(true);
        assert r.read() == 'x';
    }

    @Test
    public void testMultiple() throws IOException {
        JSONReader jsonReader = new JSONReader(new StringReader("truefalse[]{}123null"));
        assert jsonReader.read().equals(true);
        assert jsonReader.read().equals(false);
        assert jsonReader.read() instanceof List<?>;
        assert jsonReader.read() instanceof Map<?,?>;
        assert jsonReader.read().equals(new BigDecimal(123.0));
        assert jsonReader.read().equals(JSONNull.INSTANCE);
        jsonReader.expectEOF();

        jsonReader = new JSONReader(new StringReader("true false [] {} 123 null"));
        assert jsonReader.read().equals(true);
        assert jsonReader.read().equals(false);
        assert jsonReader.read() instanceof List<?>;
        assert jsonReader.read() instanceof Map<?,?>;
        assert jsonReader.read().equals(new BigDecimal(123.0));
        assert jsonReader.read().equals(JSONNull.INSTANCE);
        jsonReader.expectEOF();
    }

    @Test
    public void testReadIndented() throws IOException {
        Map<String, Object> m = (Map<String, Object>) JSONReader.readFrom(
                        "{\n" +
                        "  \"a\":123,\n" +
                        "  \"b\":{\n" +
                        "    \"x\":true,\n" +
                        "    \"y\":false\n" +
                        "  }\n" +
                        "}");
        assert m.get("a").equals(new BigDecimal(123.0));
        m = (Map<String, Object>) m.get("b");
        assert m.get("x").equals(true);
        assert m.get("y").equals(false);
    }
}
