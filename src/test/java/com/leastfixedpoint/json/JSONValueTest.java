package com.leastfixedpoint.json;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class JSONValueTest {
    @Test
    public void testAddToScalar() throws JSONTypeError {
        assertThrows(JSONTypeError.class, () -> JSONValue.wrap(123).add(234));
    }

    @Test
    public void testAddToMap() throws JSONTypeError {
        assertThrows(JSONTypeError.class, () -> JSONValue.newMap().add(234));
    }

    @Test
    public void testSetInMap() throws JSONTypeError {
        assertThrows(JSONTypeError.class, () -> JSONValue.newMap().set(0, 234));
    }

    @Test
    public void testPutInScalar() throws JSONTypeError {
        assertThrows(JSONTypeError.class, () -> JSONValue.wrap(123).put("a", 234));
    }

    @Test
    public void testPutInList() throws JSONTypeError {
        assertThrows(JSONTypeError.class, () -> JSONValue.newList().put("a", 234));
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
    public void testMapGetVariations() throws IOException {
        var xs = JSONReader.readValue("{ \"a\": 1, \"b\": 2 }");
        assert xs.isMap();
        assert xs.get("a").longValue() == 1;
        assert xs.get("z") == null;
        assert xs.get("z", 123).longValue() == 123;
        assert xs.getRequired("a").longValue() == 1;
        try {
            xs.getRequired("z");
            assert false;
        } catch (JSONSchemaError _e) {}
    }

    @Test
    public void testListGetVariations() throws IOException {
        var xs = JSONReader.readValue("[1, 2]");
        assert xs.isList();
        assert xs.get(0).longValue() == 1;
        assert xs.get(2) == null;
        assert xs.get(2, 123).longValue() == 123;
        assert xs.getRequired(0).longValue() == 1;
        try {
            xs.getRequired(2);
            assert false;
        } catch (JSONSchemaError _e) {}
    }

    @Test
    public void testEquality() {
        assert JSONValue.wrap(1).equals(JSONValue.wrap(1));
        assert JSONValue.wrap(1).equals(JSONValue.wrap((long) 1));
        assert !JSONValue.wrap(1).equals(JSONValue.wrap(1.0));
        assert !JSONValue.wrap(1).equals(JSONValue.wrap("1"));
    }

    @Test
    public void testAvoidingDoubleWrapping() {
        assert JSONValue.wrap(1).value() instanceof Number;
        assert JSONValue.wrap(JSONValue.wrap(1)).value() instanceof Number;
    }
}
