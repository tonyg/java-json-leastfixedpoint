package com.leastfixedpoint.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Helper class for concise traversal and construction of JSON values in the representation used by JSONReader
 * and JSONWriter. In general, will throw JSONTypeError when the shape of a given blob of JSON doesn't match
 * expectations.
 */
public class JSONValue implements JSONSerializable {
    protected Object blob;

    public static JSONValue wrap(Object blob) {
        return (blob == null) ? null : new JSONValue(blob);
    }

    public static Object unwrap(Object value) {
        return (value instanceof JSONValue) ? ((JSONValue) value).value() : value;
    }

    public static JSONValue newList() {
        return wrap(new ArrayList<Object>());
    }

    public static JSONValue newMap() {
        return wrap(new HashMap<String,Object>());
    }

    protected JSONValue(Object blob) {
        this.blob = blob;
    }

    public Object value() {
        return this.blob;
    }

    public String stringValue() throws JSONTypeError {
        if (blob instanceof String) return (String) blob;
        throw new JSONTypeError(String.class, blob);
    }

    public long longValue() throws JSONTypeError {
        if (blob instanceof Number) return ((Number) blob).longValue();
        throw new JSONTypeError(Number.class, blob);
    }

    public double doubleValue() throws JSONTypeError {
        if (blob instanceof Number) return ((Number) blob).doubleValue();
        throw new JSONTypeError(Number.class, blob);
    }

    // Is this a good idea?
    public BigDecimal bigDecimalValue() throws JSONTypeError {
        if (blob instanceof BigDecimal) return ((BigDecimal) blob);
        if (blob instanceof BigInteger) return new BigDecimal((BigInteger) blob);
        throw new JSONTypeError(new Class[] { BigDecimal.class, BigInteger.class }, blob);
    }

    public boolean booleanValue() throws JSONTypeError {
        if (blob instanceof Boolean) return (boolean) blob;
        throw new JSONTypeError(Boolean.class, blob);
    }

    public void checkNull() throws JSONTypeError {
        if (blob instanceof JSONNull) return;
        throw new JSONTypeError(JSONNull.class, blob);
    }

    public List<Object> listValue() throws JSONTypeError {
        if (blob instanceof List<?>) return (List<Object>) blob;
        throw new JSONTypeError(List.class, blob);
    }

    public Map<String,Object> mapValue() throws JSONTypeError {
        if (blob instanceof Map<?,?>) return (Map<String,Object>) blob;
        throw new JSONTypeError(Map.class, blob);
    }

    public JSONValue get(int index) throws JSONTypeError {
        return wrap(this.listValue().get(index));
    }

    /** Returns 'this' to allow for chaining-style building of complex values. */
    public JSONValue set(int index, Object value) throws JSONTypeError {
        this.listValue().set(index, unwrap(value));
        return this;
    }

    /** Returns 'this' to allow for chaining-style building of complex values. */
    public JSONValue add(Object value) throws JSONTypeError {
        this.listValue().add(unwrap(value));
        return this;
    }

    public JSONValue get(String key) throws JSONTypeError {
        return wrap(this.mapValue().get(key));
    }

    /** Returns 'this' to allow for chaining-style building of complex values. */
    public JSONValue put(String key, Object value) throws JSONTypeError {
        this.mapValue().put(key, unwrap(value));
        return this;
    }

    /** Returns 'this' to allow for chaining-style building of complex values. */
    public JSONValue remove(String key) throws JSONTypeError {
        this.mapValue().remove(key);
        return this;
    }

    public boolean containsKey(String key) throws JSONTypeError {
        return this.mapValue().containsKey(key);
    }

    public int size() throws JSONTypeError {
        if (blob instanceof Collection) return ((Collection) blob).size();
        if (blob instanceof Map) return ((Map) blob).size();
        throw new JSONTypeError(new Class[] { Collection.class, Map.class }, blob);
    }

    @Override
    public void jsonSerialize(JSONWriter w) throws IOException {
        w.write(blob);
    }

    @Override
    public String toString() {
        return blob.toString();
    }
}
