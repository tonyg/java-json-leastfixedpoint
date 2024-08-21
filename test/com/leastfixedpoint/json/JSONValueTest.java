package com.leastfixedpoint.json;

import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class JSONValueTest {
    @Test(expectedExceptions = {JSONTypeError.class})
    public void testAddToScalar() throws JSONTypeError {
        JSONValue.wrap(123).add(234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testAddToMap() throws JSONTypeError {
        JSONValue.newMap().add(234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testSetInMap() throws JSONTypeError {
        JSONValue.newMap().set(0, 234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testPutInScalar() throws JSONTypeError {
        JSONValue.wrap(123).put("a", 234);
    }

    @Test(expectedExceptions = {JSONTypeError.class})
    public void testPutInList() throws JSONTypeError {
        JSONValue.newList().put("a", 234);
    }

    @Test
    public void testBigDecimalValue() throws JSONTypeError {
        assert JSONValue.wrap(new BigDecimal("1e40")).bigDecimalValue().compareTo(new BigDecimal("1E+40")) == 0;
        assert JSONValue.wrap(new BigInteger("10000000000000000000000000000000000000000"))
                .bigDecimalValue().compareTo(new BigDecimal("1E+40")) == 0;
    }

    @Test
    public void testListIteration() throws IOException {
        var xs = JSONReader.readValue("[1, 2, 3, 4]");
        long sum = 0;
        for (var x : xs.list()) {
            sum += x.longValue();
        }
        assert sum == 10;
    }

    @Test
    public void testMapKeyIteration() throws IOException {
        var xs = JSONReader.readValue("{ \"a\": 1, \"b\": 2 }");
        var keys = new HashSet<String>();
        for (var x : xs.mapKeys()) keys.add(x);
        assert keys.equals(new HashSet<String>(Arrays.asList("a", "b")));
    }

    @Test
    public void testMapValueIteration() throws IOException {
        var xs = JSONReader.readValue("{ \"a\": 1, \"b\": 2 }");
        var values = new HashSet<JSONValue>();
        for (var x : xs.mapValues()) values.add(x);
        var expected = new HashSet<JSONValue>();
        expected.add(JSONValue.wrap(1));
        expected.add(JSONValue.wrap(2));
        assert values.equals(expected);
    }

    @Test
    public void testMapEntryIteration() throws IOException {
        var xs = JSONReader.readValue("{ \"a\": 1, \"b\": 2 }");
        var values = new HashSet<Map.Entry<String, JSONValue>>();
        for (var x : xs.mapEntries()) values.add(x);
        assert values.size() == 2;
        for (var e : values) {
            if (e.getKey().equals("a")) assert e.getValue().equals(JSONValue.wrap(1));
            else if (e.getKey().equals("b")) assert e.getValue().equals(JSONValue.wrap(2));
            else assert false;
        }
    }

    @Test
    public void testEquality() {
        assert JSONValue.wrap(1).equals(JSONValue.wrap(1));
        assert JSONValue.wrap(1).equals(JSONValue.wrap((long) 1));
        assert !JSONValue.wrap(1).equals(JSONValue.wrap(1.0));
        assert !JSONValue.wrap(1).equals(JSONValue.wrap("1"));
    }
}
