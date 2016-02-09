package com.leastfixedpoint.json;

import org.testng.annotations.Test;

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
    }

    @Test
    public void testSimple() throws JSONSerializationError {
        checkWrite(true, "true");
        checkWrite(false, "false");
        checkWrite(null, "null");
    }

    @Test
    public void testArray() throws JSONSerializationError {
        checkWrite(new Object[] {}, "[]");
        checkWrite(new Object[] {1.0, null, "C"}, "[1,null,\"C\"]");
        List<Object> a = new ArrayList<>();
        a.add(1.0);
        a.add(null);
        a.add("C");
        checkWrite(a, "[1,null,\"C\"]");
        checkWrite(new Object[] { new Object[] { new Object [] {} } }, "[[[]]]");
    }

    @Test
    public void testMap() throws JSONSerializationError {
        checkWrite(new HashMap(), "{}");
        Map<String,Object> m = new HashMap<>();
        m.put("a", 123.0);
        checkWrite(m, "{\"a\":123}");
        m.put("b", new Object[] { null });
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
}