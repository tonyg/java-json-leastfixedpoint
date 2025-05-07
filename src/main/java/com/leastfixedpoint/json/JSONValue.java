package com.leastfixedpoint.json;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Helper class for concise traversal and construction of JSON values in the representation used by {@link JSONReader}
 * and {@link JSONWriter}. In general, will throw {@link JSONTypeError} when the shape of a given blob of JSON doesn't
 * match expectations.
 */
public class JSONValue implements JSONSerializable {
    protected Object blob;

    /** Returns a JSONValue wrapping the given object, unless the argument is null or already a JSONValue, in which case it is returned directly. */
    public static JSONValue wrap(Object blob) {
        return (blob == null || blob instanceof JSONValue) ? (JSONValue) blob : new JSONValue(blob);
    }

    /** If given a JSONValue, extracts the underlying object; otherwise, simply returns the value given. */
    public static Object unwrap(Object value) {
        return (value instanceof JSONValue) ? ((JSONValue) value).value() : value;
    }

    /** Construct a new {@link List}, suitable for use as a JSON value. */
    public static JSONValue newList() {
        return wrap(new ArrayList<Object>());
    }

    /** Construct a new {@link Map}, suitable for use as a JSON value. */
    public static JSONValue newMap() {
        return wrap(new HashMap<String,Object>());
    }

    protected JSONValue(Object blob) {
        if (blob instanceof Number) {
            if (blob instanceof BigDecimal) {
                this.blob = blob;
            } else if (blob instanceof Float || blob instanceof Double) {
                this.blob = ((Number) blob).doubleValue();
            } else if (blob instanceof BigInteger) {
                this.blob = new BigDecimal((BigInteger) blob);
            } else {
                this.blob = new BigDecimal(((Number) blob).longValue());
            }
        } else {
            this.blob = blob;
        }
    }

    /** Extract the underlying value contained in this object. */
    public Object value() {
        return this.blob;
    }

    /** Answers true iff the underlying value is a String. */
    public boolean isString() {
        return blob instanceof String;
    }

    /** Cast the underlying value to {@link String}.
     * @throws JSONTypeError if it is not a string. */
    public String stringValue() throws JSONTypeError {
        if (isString()) return (String) blob;
        throw new JSONTypeError(String.class, blob);
    }

    /** Answers true iff the underlying value is a Number. */
    public boolean isNumber() {
        return blob instanceof Number;
    }

    /** Extract a long value from an underlying {@link Number}.
     *  @throws JSONTypeError if it is not a number. */
    public long longValue() throws JSONTypeError {
        if (isNumber()) return ((Number) blob).longValue();
        throw new JSONTypeError(Number.class, blob);
    }

    /** Extract a double value from an underlying {@link Number}.
     *  @throws JSONTypeError if it is not a number. */
    public double doubleValue() throws JSONTypeError {
        if (isNumber()) return ((Number) blob).doubleValue();
        throw new JSONTypeError(Number.class, blob);
    }

    // Is this a good idea?
    /** Extract a {@link BigDecimal} value from the underlying object.
     * @throws JSONTypeError if it is not a {@link BigDecimal}. */
    public BigDecimal bigDecimalValue() throws JSONTypeError {
        if (blob instanceof BigDecimal) return ((BigDecimal) blob);
        throw new JSONTypeError(BigDecimal.class, blob);
    }

    /** Answers true iff the underlying value is a Boolean. */
    public boolean isBoolean() {
        return blob instanceof Boolean;
    }

    /** Extract a boolean value from an underlying {@link Boolean}.
     * @throws JSONTypeError if it is not a boolean. */
    public boolean booleanValue() throws JSONTypeError {
        if (isBoolean()) return (boolean) blob;
        throw new JSONTypeError(Boolean.class, blob);
    }

    /** Answers true iff the underlying value is a JSON null (i.e., {@link JSONNull#INSTANCE}). */
    public boolean isNull() {
        return blob instanceof JSONNull;
    }

    /** @throws JSONTypeError if the underlying object is not a JSON null (i.e., {@link JSONNull#INSTANCE}). */
    public void checkNull() throws JSONTypeError {
        if (isNull()) return;
        throw new JSONTypeError(JSONNull.class, blob);
    }

    /** Answers true iff the underlying value is a {@link List}. */
    public boolean isList() {
        return blob instanceof List<?>;
    }

    /** Cast the underlying value to {@link List}.
     * @throws JSONTypeError if it is not a list. */
    public List<Object> listValue() throws JSONTypeError {
        if (isList()) {
            @SuppressWarnings("unchecked")
            var xs = (List<Object>) blob;
            return xs;
        }
        throw new JSONTypeError(List.class, blob);
    }

    /** Iterate over JSONValue-wrapped items in the underlying value, which is cast to a {@link List}
     * @throws JSONTypeError if the underlying value is not a list. */
    public Iterable<JSONValue> list() throws JSONTypeError {
        return listIterable(listValue());
    }

    protected Iterable<JSONValue> listIterable(Collection<Object> xs) throws JSONTypeError {
        return () -> {
            var i = xs.iterator();
            return new Iterator<JSONValue>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public JSONValue next() {
                    return wrap(i.next());
                }
            };
        };
    }

    /** Answers true iff the underlying value is a {@link Map}. */
    public boolean isMap() {
        return blob instanceof Map<?,?>;
    }

    /** Cast the underlying value to {@link Map}.
     * @throws JSONTypeError if it is not a map. */
    public Map<String,Object> mapValue() throws JSONTypeError {
        if (isMap()) {
            @SuppressWarnings("unchecked")
            var m = (Map<String,Object>) blob;
            return m;
        }
        throw new JSONTypeError(Map.class, blob);
    }

    /** Iterate over keys of the underlying value, which is cast to a {@link Map}
     * @throws JSONTypeError if the underlying value is not a map. */
    public Iterable<String> mapKeys() throws JSONTypeError {
        return mapValue().keySet();
    }

    /** Iterate over JSONValue-wrapped entries in the underlying value, which is cast to a {@link Map}
     * @throws JSONTypeError if the underlying value is not a map. */
    public Iterable<JSONValue> mapValues() throws JSONTypeError {
        return listIterable(mapValue().values());
    }

    /** Iterate over JSONValue-wrapped entries in the underlying value, which is cast to a {@link Map}
     * @throws JSONTypeError if the underlying value is not a map. */
    public Iterable<Map.Entry<String,JSONValue>> mapEntries() throws JSONTypeError {
        var m = mapValue().entrySet();
        return () -> {
            var i = m.iterator();
            return new Iterator<Map.Entry<String,JSONValue>>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public Map.Entry<String,JSONValue> next() {
                    var e = i.next();
                    return new Map.Entry<String,JSONValue>() {
                        @Override
                        public String getKey() {
                            return e.getKey();
                        }

                        @Override
                        public JSONValue getValue() {
                            return wrap(e.getValue());
                        }

                        @Override
                        public JSONValue setValue(JSONValue value) {
                            return wrap(e.setValue(value.value()));
                        }
                    };
                }
            };
        };
    }

    /** Retrieve the object at the index'th position in the underlying list.
     * @throws JSONTypeError if the underlying object is not a {@link List}.
     */
    public JSONValue get(int index) throws JSONTypeError {
        final var xs = this.listValue();
        return (index < xs.size()) ? wrap(xs.get(index)) : null;
    }

    /** Retrieve the object at the index'th position in the underlying list, or
     * the given default value if there is no such index.
     * @throws JSONTypeError if the underlying object is not a {@link List}.
     */
    public JSONValue get(int index, Object defaultValue) throws JSONTypeError {
        final var xs = this.listValue();
        return wrap(index < xs.size() ? xs.get(index) : defaultValue);
    }

    /** Retrieve the object at the index'th position in the underlying list, throwing
     * JSONMissingIndexError if there is no such index.
     * @throws JSONTypeError if the underlying object is not a {@link List}
     * @throws JSONMissingIndexError if the list is missing the requested index
     */
    public JSONValue getRequired(int index) throws JSONSchemaError {
        final var v = get(index);
        if (v == null) throw new JSONMissingIndexError(index, this.listValue());
        return v;
    }

    /** Replace the object at the index'th position in the underlying list.
     * Returns 'this' to allow for chaining-style building of complex values.
     * @throws JSONTypeError if the underlying object is not a {@link List}.
     */
    public JSONValue set(int index, Object value) throws JSONTypeError {
        this.listValue().set(index, unwrap(value));
        return this;
    }

    /** Append an object to the underlying list.
     * Returns 'this' to allow for chaining-style building of complex values.
     * @throws JSONTypeError if the underlying object is not a {@link List}.
     */
    public JSONValue add(Object value) throws JSONTypeError {
        this.listValue().add(unwrap(value));
        return this;
    }

    /** Retrieve the object at the given key in the underlying map.
     * @throws JSONTypeError if the underlying object is not a {@link Map}.
     */
    public JSONValue get(String key) throws JSONTypeError {
        return wrap(this.mapValue().get(key));
    }

    /** Retrieve the object at the given key in the underlying map, or
     * the given default value if there is no such key.
     * @throws JSONTypeError if the underlying object is not a {@link Map}.
     */
    public JSONValue get(String key, Object defaultValue) throws JSONTypeError {
        final var v = this.mapValue().get(key);
        return wrap(v == null ? defaultValue : v);
    }

    /** Retrieve the object at the given key in the underlying map, throwing
     * JSONMissingKeyError if there is no such key.
     * @throws JSONTypeError if the underlying object is not a {@link Map}
     * @throws JSONMissingKeyError if the map is missing the requested key
     */
    public JSONValue getRequired(String key) throws JSONSchemaError {
        final var v = get(key);
        if (v == null) throw new JSONMissingKeyError(key, this.mapValue());
        return v;
    }

    /** Replace the object at the given key in the underlying map.
     * Returns 'this' to allow for chaining-style building of complex values.
     * @throws JSONTypeError if the underlying object is not a {@link Map}.
     */
    public JSONValue put(String key, Object value) throws JSONTypeError {
        this.mapValue().put(key, unwrap(value));
        return this;
    }

    /** Remove the object at the given key in the underlying map.
     * Returns 'this' to allow for chaining-style building of complex values.
     * @throws JSONTypeError if the underlying object is not a {@link Map}.
     */
    public JSONValue remove(String key) throws JSONTypeError {
        this.mapValue().remove(key);
        return this;
    }

    /** Tests whether there is an object at the given key in the underlying map.
     * @throws JSONTypeError if the underlying object is not a {@link Map}.
     */
    public boolean containsKey(String key) throws JSONTypeError {
        return this.mapValue().containsKey(key);
    }

    /** Returns the number of elements held in the underlying {@link List} ({@link Collection}) or {@link Map}.
     * @throws JSONTypeError if the underlying object is not any of these.
     */
    public int size() throws JSONTypeError {
        if (blob instanceof Collection) return ((Collection<?>) blob).size();
        if (blob instanceof Map) return ((Map<?,?>) blob).size();
        throw new JSONTypeError(new Class[] { Collection.class, Map.class }, blob);
    }

    @Override
    public <W extends Writer> void jsonSerialize(JSONWriter<W> w) throws IOException {
        w.write(blob);
    }

    @Override
    public String toString() {
        return blob.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other.equals(blob);
    }

    @Override
    public int hashCode() {
        return blob.hashCode();
    }
}
