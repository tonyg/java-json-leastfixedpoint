package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class JSONWriterTest {
    public void checkWrite(Object o, String expected) throws JSONSerializationError {
        String actual = JSONWriter.writeToString(o);
        assert actual.equals(expected) : "Actual >>>" + actual + "<<< =/= >>>" + expected + "<<<";
    }

    public void checkWriteIndented(Object o, String expected) throws JSONSerializationError {
        String actual = JSONWriter.writeToString(o, true);
        assert actual.equals(expected) : "Actual >>>" + actual + "<<< =/= >>>" + expected + "<<<";
    }

    @Test
    public void testNumbers() throws JSONSerializationError {
        checkWrite(0, "0");
        checkWrite(123.0, "123");
        checkWrite(123.125, "123.125");
        checkWrite(-123.0, "-123");
        checkWrite(-123.125, "-123.125");
        checkWrite(-12300.0, "-12300");
        checkWrite(-1.23E09, "-1.23E9");
        checkWrite(1.23E09, "1.23E9");
        checkWrite(-1.23E-13, "-1.23E-13");
        checkWrite(1.23E-13, "1.23E-13");
        checkWrite(JSONValue.wrap(1.23E-13), "1.23E-13");
        checkWrite(new BigDecimal("1000000000000000000000000000000000000000"),
                "1000000000000000000000000000000000000000");
        checkWrite(new BigDecimal("1e40"), "1E+40");
        checkWrite(new BigDecimal("1234567890123456789012345678901234567890"),
                "1234567890123456789012345678901234567890");
    }

    @Test
    public void testStrings() throws JSONSerializationError {
        checkWrite("123", "\"123\"");
        checkWrite("", "\"\"");
        checkWrite("\\", "\"\\\\\"");
        checkWrite("\"", "\"\\\"\"");
        checkWrite("x\"x", "\"x\\\"x\"");
        checkWrite("\n", "\"\\n\"");
        checkWrite("\uD834\uDD1E", "\"\uD834\udd1e\"");
        checkWrite("\u0080", "\"\\u0080\"");
        checkWrite(JSONValue.wrap("hello"), "\"hello\"");
    }

    @Test
    public void testSimple() throws JSONSerializationError {
        checkWrite(true, "true");
        checkWrite(false, "false");
        checkWrite(JSONNull.INSTANCE, "null");
        checkWrite(JSONValue.wrap(true), "true");
        checkWrite(JSONValue.wrap(JSONNull.INSTANCE), "null");
    }

    @Test
    public void testArray() throws JSONSerializationError {
        checkWrite(new Object[] {}, "[]");
        checkWrite(new Object[] {1.0, JSONNull.INSTANCE, "C"}, "[1,null,\"C\"]");
        List<Object> a = new ArrayList<>();
        a.add(1.0);
        a.add(JSONNull.INSTANCE);
        a.add("C");
        checkWrite(a, "[1,null,\"C\"]");
        checkWrite(new Object[] { new Object[] { new Object [] {} } }, "[[[]]]");
    }

    @Test
    public void testValueWriting() throws JSONTypeError, JSONSerializationError {
        checkWrite(JSONValue.newList()
                        .add(1.0)
                        .add(JSONNull.INSTANCE)
                        .add("C")
                        .add(JSONValue.newList().add(JSONValue.newMap()
                                .put("x", true)
                                .put("y", JSONValue.newMap().put("z", "z")))),
                "[1,null,\"C\",[{\"x\":true,\"y\":{\"z\":\"z\"}}]]");
    }

    @Test(expectedExceptions = {JSONSerializationError.class})
    public void testWriteNullInArray() throws JSONSerializationError {
        checkWrite(new Object[]{1.0, null, "C"}, "[1,null,\"C\"]");
    }

    @Test(expectedExceptions = {JSONSerializationError.class})
    public void testWriteNullInArrayList() throws JSONSerializationError {
        List<Object> a = new ArrayList<>();
        a.add(1.0);
        a.add(null);
        a.add("C");
        checkWrite(a, "[1,null,\"C\"]");
    }

    @Test
    public void testMap() throws JSONSerializationError {
        checkWrite(new HashMap(), "{}");
        Map<String,Object> m = new HashMap<>();
        m.put("a", 123.0);
        checkWrite(m, "{\"a\":123}");
        m.put("b", new Object[] { JSONNull.INSTANCE });
        checkWrite(m, "{\"a\":123,\"b\":[null]}");
        m.put("b", 234.0);
        checkWrite(m, "{\"a\":123,\"b\":234}");
        Map<String,Object> m2 = new HashMap<>();
        m2.put("x", true);
        m2.put("y", false);
        m.put("b", m2);
        checkWrite(m, "{\"a\":123,\"b\":{\"x\":true,\"y\":false}}");
        checkWriteIndented(m,
                "{\n" +
                "  \"a\":123,\n" +
                "  \"b\":{\n" +
                "    \"x\":true,\n" +
                "    \"y\":false\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testMapUnsorted() throws IOException {
        Map<String,Object> m = new HashMap<>();
        m.put("a", 123.0);
        m.put("b", new Object[] { JSONNull.INSTANCE });

        JSONWriter jw = new JSONWriter(new StringWriter());
        jw.setSortKeys(false);
        jw.write(m);
        String actual = ((StringWriter) jw.getWriter()).getBuffer().toString();
        assert actual.equals("{\"a\":123,\"b\":[null]}") ||
                actual.equals("{\"b\":[null],\"a\":123}");

        m.put("b", 234.0);
        jw = new JSONWriter(new StringWriter());
        jw.setSortKeys(false);
        jw.write(m);
        actual = ((StringWriter) jw.getWriter()).getBuffer().toString();
        assert actual.equals("{\"a\":123,\"b\":234}") ||
                actual.equals("{\"b\":234,\"a\":123}");
    }

    @Test(expectedExceptions = {JSONSerializationError.class})
    public void testWriteNullInMap() throws JSONSerializationError {
        Map<String,Object> m = new HashMap<>();
        m.put("a", 123.0);
        m.put("b", new Object[] { null });
        checkWrite(m, "{\"a\":123,\"b\":[null]}");
    }

    @Test(expectedExceptions = {JSONSerializationError.class})
    public void testInvalidObject1() throws JSONSerializationError {
        checkWrite(new Object(), "uh oh this shouldn't yield any answer");
    }

    @Test(expectedExceptions = {JSONSerializationError.class})
    public void testInvalidObject2() throws JSONSerializationError {
        Map<Object,Object> m = new HashMap<>();
        m.put(123, 123.0);
        checkWrite(m, "uh oh this shouldn't yield any answer");
    }

    @Test(expectedExceptions = {JSONSerializationError.class})
    public void testInvalidObject3() throws IOException {
        Map<Object,Object> m = new HashMap<>();
        m.put(123, 123.0);
        JSONWriter jw = new JSONWriter(new StringWriter());
        jw.setSortKeys(false);
        jw.write(m);
    }
}
